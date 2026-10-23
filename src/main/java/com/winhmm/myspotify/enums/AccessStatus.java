package com.winhmm.myspotify.enums;

/*
    =====================================================
    ACCESSSTATUS - QUYỀN VÀO WEB
    =====================================================

    - ACCEPT: được đăng nhập và dùng web.
    - REFUSE: không đăng nhập được (mặc định của mọi tài khoản mới đăng ký).

    Chỉ chủ dự án đổi được, bằng câu SQL trực tiếp trong database.
    Không có API nào đổi giá trị này, kể cả API của Admin.
*/
public enum AccessStatus {
    ACCEPT,
    REFUSE
}