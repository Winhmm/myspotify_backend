package com.winhmm.myspotify.service;

import com.winhmm.myspotify.dto.lyrics.LyricLine;
import com.winhmm.myspotify.dto.lyrics.LyricsResponse;
import com.winhmm.myspotify.entity.Song;
import com.winhmm.myspotify.entity.SongLyrics;
import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.Role;
import com.winhmm.myspotify.enums.SongStatus;
import com.winhmm.myspotify.repository.SongLyricsRepository;
import com.winhmm.myspotify.repository.SongRepository;
import com.winhmm.myspotify.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    =====================================================================
    LYRICSSERVICE - NGHIỆP VỤ LỜI BÀI HÁT
    =====================================================================

    Các chức năng:
        getPublishedLyrics(...) → người nghe xem lời của bài đã phát hành
        getMyLyrics(...)        → Artist xem lời bài của mình (để sửa)
        saveLyrics(...)         → Artist thêm mới hoặc thay toàn bộ lời
        deleteLyrics(...)       → Artist xóa lời

    Được gọi từ LyricsController.

    =====================================================================
    2 CÁCH ARTIST NHẬP LỜI (FRONTEND ĐÃ LÀM), BACKEND NHẬN CÙNG 1 KIỂU
    =====================================================================

    Cách 1 - Artist nhớ thời gian: dán thẳng lời có sẵn thời gian.
        [00:27.80] Ngày xưa lúc thơ bé, ta thường nhìn mưa hát
        [00:32.90] Và khi cơn mưa như nặng hạt...

    Cách 2 - Artist không nhớ: dán lời không có thời gian, vừa nghe nhạc vừa bấm "Gắn mốc".
        Frontend ghi lại thời điểm mỗi lần bấm, rồi tự ghép thành đúng dạng như cách 1.

    → Cả 2 cách đều gửi lên cùng 1 chuỗi LRC trong trường "content".
      Backend không cần biết Artist đã dùng cách nào.

    =====================================================================
    BACKEND LÀM GÌ VỚI CHUỖI LRC?
    =====================================================================

    1. Đọc từng dòng, tách thời gian và chữ (hàm parseLyrics).
    2. Kiểm tra đúng quy tắc (xem bên dưới). Sai → báo lỗi 400, không lưu gì.
    3. Ghép lại thành chuỗi chuẩn (hàm toContent): bỏ dòng trống,
       đưa mọi thời gian về cùng 1 dạng [mm:ss.xx].
    4. Lưu chuỗi chuẩn vào bảng song_lyrics.
    5. Khi frontend xin lời → đọc chuỗi đã lưu, tách lại thành từng câu (timeMs + text) để trả về.

    =====================================================================
    QUY TẮC KIỂM TRA
    =====================================================================

    - Có ít nhất 1 dòng chữ, tối đa 300 dòng, mỗi dòng tối đa 200 ký tự.
    - Hoặc tất cả các dòng có thời gian, hoặc không dòng nào có.
      (Có dòng có, dòng không → trình phát nhạc không biết lúc nào hiện các dòng thiếu.)
    - Thời gian phải hợp lệ: giây từ 00 đến 59.
    - Thời gian dòng sau phải lớn hơn dòng trước (lời phải đi theo thứ tự bài hát).
    - Thời gian không vượt quá độ dài bài hát.

    Lời không cần Admin duyệt lại: sửa lời không đổi trạng thái bài hát.
*/
@Service
public class LyricsService {
    private static final int MAX_LINES = 300;
    private static final int MAX_LINE_LENGTH = 200;

    /*
        ---------------------------------------------------------------
        Mẫu nhận diện dòng có thời gian (biểu thức chính quy - regex)
        ---------------------------------------------------------------
        Khớp các dạng: [00:27.80] chữ   [0:27.8] chữ   [00:27.805] chữ   [00:27] chữ   [00:27:80] chữ

        Giải thích từng phần của  ^\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\](.*)$
            ^ và $          : khớp từ đầu tới cuối dòng.
            \[ và \]        : dấu ngoặc vuông thật (phải thêm \ vì [ ] có nghĩa đặc biệt trong regex).
            (\d{1,3})       : nhóm 1 = phút, 1 đến 3 chữ số.
            :               : dấu hai chấm.
            (\d{2})         : nhóm 2 = giây, đúng 2 chữ số.
            (?:[.:](\d{1,3}))? : phần lẻ của giây, có thể không có (dấu ? ở cuối).
                                 Bắt đầu bằng dấu chấm hoặc hai chấm, nhóm 3 = 1 đến 3 chữ số.
                                 (?: ...) là nhóm chỉ để gom, không lấy giá trị.
            (.*)            : nhóm 4 = phần chữ còn lại của dòng.

        Trong chuỗi Java phải viết \\ thay cho \ (vì \ trong chuỗi Java cũng là ký tự đặc biệt).

        Pattern.compile chạy 1 lần khi class được nạp, dùng lại cho mọi lần đọc (nhanh hơn compile mỗi lần).
    */
    private static final Pattern TIMED_LINE =
            Pattern.compile("^\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\](.*)$");

    /*
        ---------------------------------------------------------------
        Mẫu nhận diện dòng thông tin của file LRC (bỏ qua, không phải lời)
        ---------------------------------------------------------------
        File LRC tải trên mạng thường có vài dòng đầu kiểu:
            [ar:Bảo Thy]          (ca sĩ)
            [ti:Công chúa bong bóng] (tên bài)
            [by:...]              (người soạn lời)
        Dạng chung: [chữ cái:...] → bỏ qua để Artist dán nguyên file vẫn được.

        Dòng thời gian [00:27.80] bắt đầu bằng chữ số nên không bị nhầm.
    */
    private static final Pattern TAG_LINE = Pattern.compile("^\\[[a-zA-Z#]+:.*\\]$");

    private final SongLyricsRepository songLyricsRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final SongService songService;

    public LyricsService(SongLyricsRepository songLyricsRepository,
                         SongRepository songRepository,
                         UserRepository userRepository,
                         SongService songService) {
        this.songLyricsRepository = songLyricsRepository;
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.songService = songService;
    }

    /*
        ---------------------------------------------------------------
        Người nghe xem lời
        ---------------------------------------------------------------
        Chỉ bài đã phát hành mới xem được lời.
        songService.getPublishedSong dùng lại đúng điều kiện của UC22:
        bài chưa phát hành, bị ẩn, bị gỡ, Artist bị khóa → "Song does not exist".
    */
    public LyricsResponse getPublishedLyrics(Long songId) {
        Song song = songService.getPublishedSong(songId);
        return toResponse(song);
    }

    /*
        ---------------------------------------------------------------
        Artist xem lời bài của mình
        ---------------------------------------------------------------
        Xem được bài ở mọi trạng thái (trừ đã xóa), kể cả bài đang chờ duyệt.
        Dùng khi Artist mở hộp thoại sửa lời: frontend điền "content" vào ô nhập.
    */
    public LyricsResponse getMyLyrics(Long artistId, Long songId) {
        Song song = getMySong(artistId, songId);
        return toResponse(song);
    }

    /*
        ---------------------------------------------------------------
        Artist thêm mới hoặc thay toàn bộ lời
        ---------------------------------------------------------------
        Các bước:
        1. Kiểm tra Artist đang hoạt động, bài thuộc về Artist.
        2. Đọc và kiểm tra lời (parseLyrics). Sai → ném lỗi, chưa lưu gì.
        3. Bài đã có lời → lấy dòng cũ ra sửa. Chưa có → tạo dòng mới.
           .orElse(new SongLyrics()): Optional rỗng thì dùng object mới.
        4. Lưu lời ở dạng chuẩn.

        save() tự biết INSERT hay UPDATE:
        - Object mới (id = null) → INSERT.
        - Object lấy từ database (đã có id) → UPDATE.
    */
    public LyricsResponse saveLyrics(Long artistId, Long songId, String content) {
        getArtist(artistId);
        Song song = getMySong(artistId, songId);

        List<LyricLine> lines = parseLyrics(content, song);

        SongLyrics lyrics = songLyricsRepository.findBySongId(songId).orElse(new SongLyrics());
        lyrics.setSong(song);
        lyrics.setContent(toContent(lines));

        SongLyrics saved = songLyricsRepository.save(lyrics);
        return toResponse(song, saved);
    }

    /*
        ---------------------------------------------------------------
        Artist xóa lời
        ---------------------------------------------------------------
        Bài chưa có lời → báo lỗi, giống "Song has no canvas video" khi gỡ video.
    */
    public void deleteLyrics(Long artistId, Long songId) {
        getArtist(artistId);
        getMySong(artistId, songId);

        SongLyrics lyrics = songLyricsRepository.findBySongId(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song has no lyrics"));

        songLyricsRepository.delete(lyrics);
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: đọc lời của 1 bài rồi đổi sang LyricsResponse
        ---------------------------------------------------------------
        .orElse(null): bài chưa có lời → lyrics = null.
    */
    private LyricsResponse toResponse(Song song) {
        SongLyrics lyrics = songLyricsRepository.findBySongId(song.getId()).orElse(null);
        return toResponse(song, lyrics);
    }

    /*
        Cùng tên với hàm trên nhưng khác tham số (gọi là overload, Java tự chọn đúng hàm theo tham số).
        Dùng khi đã có sẵn lyrics trong tay (VD vừa lưu xong), khỏi đọc lại database.

        Chưa có lời → trả response rỗng (lines = [], synced = false), không báo lỗi.

        synced = true khi câu đầu tiên có thời gian.
        Chỉ cần xem câu đầu vì lúc lưu đã đảm bảo: hoặc tất cả có thời gian, hoặc không câu nào có.
        Không cần lưu cột synced riêng trong database: tính lại từ lời là ra.
    */
    private LyricsResponse toResponse(Song song, SongLyrics lyrics) {
        if(lyrics == null) {
            return new LyricsResponse(song.getId(), false, "", new ArrayList<>(), null);
        }

        List<LyricLine> lines = parseLyrics(lyrics.getContent(), song);
        boolean synced = lines.get(0).getTimeMs() != null;

        return new LyricsResponse(song.getId(), synced, lyrics.getContent(), lines, lyrics.getUpdatedAt());
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: đọc chuỗi lời thành danh sách câu, đồng thời kiểm tra quy tắc
        ---------------------------------------------------------------
        Dùng ở 2 chỗ:
        - Lúc lưu: kiểm tra chuỗi Artist gửi lên.
        - Lúc đọc: tách chuỗi đã lưu thành từng câu. Chuỗi đã lưu luôn đúng quy tắc
          (đã kiểm tra lúc lưu), nên lúc đọc không bao giờ báo lỗi.

        Ví dụ đọc dòng "[01:08.30] Ngày xưa rất xưa ấy":
            nhóm 1 = "01"  → 1 phút
            nhóm 2 = "08"  → 8 giây
            nhóm 3 = "30"  → phần lẻ
            nhóm 4 = " Ngày xưa rất xưa ấy" → trim() → "Ngày xưa rất xưa ấy"
            timeMs = (1 * 60 + 8) * 1000 + 300 = 68300

        Đổi phần lẻ sang mili giây, chỉ giữ 2 chữ số đầu (phần trăm giây):
            "3"   → "30" → 30 * 10 = 300 ms
            "30"  → "30" → 300 ms
            "305" → "30" → 300 ms (bỏ chữ số thứ 3)
            không có → "00" → 0 ms
        Cách làm: thêm "00" vào cuối rồi cắt lấy 2 ký tự đầu.
        Vì sao bỏ chữ số thứ 3? Chuẩn LRC dùng phần trăm giây, và chuỗi lưu lại cũng dạng [mm:ss.xx].
        Đọc đúng độ chính xác lúc lưu thì lúc đọc lại ra y hệt.

        rowNumber: số thứ tự dòng trong ô nhập (đếm cả dòng trống),
        để báo lỗi Artist biết chính xác dòng nào sai.
    */
    private List<LyricLine> parseLyrics(String content, Song song) {
        /*
            Độ dài bài hát lưu theo giây và đã làm tròn xuống (VD 4 phút 51,9 giây → 291 giây).
            Cộng thêm 1 giây để không chặn oan câu hát nằm trong phần lẻ cuối bài.
        */
        long maxTimeMs = (song.getDurationSeconds() + 1) * 1000L;

        List<LyricLine> lines = new ArrayList<>();
        int timedCount = 0;
        Long previousTimeMs = null;

        /*
            Tách theo xuống dòng. "\\r?\\n" khớp cả "\n" (Linux, Mac) và "\r\n" (Windows).
        */
        String[] rows = content.split("\\r?\\n");

        for(int i = 0; i < rows.length; i++) {
            int rowNumber = i + 1;
            String row = rows[i].trim();

            /*
                Bỏ qua dòng trống và dòng thông tin [ar:...], [ti:...].
            */
            if(row.isEmpty() || TAG_LINE.matcher(row).matches()) {
                continue;
            }

            Matcher matcher = TIMED_LINE.matcher(row);
            Long timeMs = null;
            String text = row;

            if(matcher.matches()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                String fraction = matcher.group(3) == null ? "" : matcher.group(3);
                int centiseconds = Integer.parseInt((fraction + "00").substring(0, 2));

                if(seconds >= 60) {
                    throw new IllegalArgumentException("Line " + rowNumber + ": invalid time, seconds must be 00 - 59");
                }

                timeMs = (minutes * 60L + seconds) * 1000 + centiseconds * 10L;
                text = matcher.group(4).trim();

                if(timeMs > maxTimeMs) {
                    throw new IllegalArgumentException("Line " + rowNumber + ": time is longer than the song");
                }

                if(previousTimeMs != null && timeMs <= previousTimeMs) {
                    throw new IllegalArgumentException(
                            "Line " + rowNumber + ": time must be greater than the previous line");
                }

                previousTimeMs = timeMs;
                timedCount++;
            }

            if(text.length() > MAX_LINE_LENGTH) {
                throw new IllegalArgumentException(
                        "Line " + rowNumber + ": must not exceed " + MAX_LINE_LENGTH + " characters");
            }

            lines.add(new LyricLine(timeMs, text));
        }

        if(lines.isEmpty()) {
            throw new IllegalArgumentException("Lyrics must not be empty");
        }

        if(lines.size() > MAX_LINES) {
            throw new IllegalArgumentException("Lyrics must not exceed " + MAX_LINES + " lines");
        }

        /*
            Có dòng có thời gian, có dòng không → báo kèm số lượng để Artist biết còn thiếu bao nhiêu.
            VD Artist mới gắn mốc 2/48 dòng rồi bấm Lưu
            → "Only 2/48 lines have a time. Either all lines must have a time or none".
        */
        if(timedCount > 0 && timedCount < lines.size()) {
            throw new IllegalArgumentException("Only " + timedCount + "/" + lines.size()
                    + " lines have a time. Either all lines must have a time or none");
        }

        return lines;
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: ghép danh sách câu thành chuỗi chuẩn để lưu
        ---------------------------------------------------------------
        VD 2 câu:
            (27800, "Ngày xưa lúc thơ bé")  → "[00:27.80] Ngày xưa lúc thơ bé"
            (32900, "Và khi cơn mưa")       → "[00:32.90] Và khi cơn mưa"
        Ghép bằng "\n" → mỗi câu 1 dòng.

        StringBuilder: ghép nhiều chuỗi nhanh hơn dùng dấu + trong vòng lặp
        (dấu + tạo chuỗi mới mỗi lần, StringBuilder chỉ nối thêm vào cùng 1 chỗ).
    */
    private String toContent(List<LyricLine> lines) {
        StringBuilder builder = new StringBuilder();

        for(LyricLine line : lines) {
            if(builder.length() > 0) {
                builder.append("\n");
            }

            if(line.getTimeMs() != null) {
                builder.append(formatTime(line.getTimeMs()));

                if(!line.getText().isEmpty()) {
                    builder.append(" ");
                }
            }

            builder.append(line.getText());
        }

        return builder.toString();
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: đổi mili giây thành dạng [mm:ss.xx]
        ---------------------------------------------------------------
        VD 68300 ms:
            phút          = 68300 / 60000        = 1
            giây          = (68300 / 1000) % 60  = 68 % 60 = 8
            phần trăm giây = (68300 % 1000) / 10 = 300 / 10 = 30
            → "[01:08.30]"

        Phép chia giữa 2 số nguyên trong Java tự bỏ phần lẻ (68300 / 60000 = 1).
        % là phép lấy số dư (68 % 60 = 8).
        %02d: in số nguyên, đủ 2 chữ số, thiếu thì thêm số 0 phía trước (8 → "08").
    */
    private String formatTime(long timeMs) {
        long minutes = timeMs / 60000;
        long seconds = (timeMs / 1000) % 60;
        long centiseconds = (timeMs % 1000) / 10;

        return String.format("[%02d:%02d.%02d]", minutes, seconds, centiseconds);
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: lấy Artist và kiểm tra đang hoạt động
        ---------------------------------------------------------------
        Giống hệt hàm cùng tên trong ArtistSongService.
        Hàm bên đó là private nên không gọi được từ đây. Viết lại vài dòng
        thay vì sửa ArtistSongService, để file đã chạy ổn không phải đụng tới.
    */
    private User getArtist(Long artistId) {
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));

        if(artist.getRole() != Role.ARTIST || artist.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active artists can manage songs");
        }

        return artist;
    }

    /*
        ---------------------------------------------------------------
        Hàm hỗ trợ: lấy bài hát của chính Artist đang đăng nhập
        ---------------------------------------------------------------
        Giống hàm cùng tên trong ArtistSongService:
        bài của người khác hoặc đã xóa → "Song does not exist".
    */
    private Song getMySong(Long artistId, Long songId) {
        Song song = songRepository.findByIdAndArtistId(songId, artistId)
                .orElseThrow(() -> new IllegalArgumentException("Song does not exist"));

        if(song.getStatus() == SongStatus.DELETED) {
            throw new IllegalArgumentException("Song does not exist");
        }

        return song;
    }
}