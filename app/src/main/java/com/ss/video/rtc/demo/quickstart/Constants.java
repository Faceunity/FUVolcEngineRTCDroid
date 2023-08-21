package com.ss.video.rtc.demo.quickstart;

/**
 * VolcEngineRTC 常量定义
 *
 * 测试房间ID：1
 * userid：1，对应TOKEN_1
 * userid：2，对应TOKEN_2
 */
public class Constants {

    //APPID 使用SDK前需要为自己的应用申请一个AppId，详情参见{https://www.volcengine.com/docs/6348/69865}
    public static final String APPID = "";

    //TOKEN 加入房间的时候需要使用token完成鉴权，详情参见{https://www.volcengine.com/docs/6348/70121}

    public static final String ROOM_ID = "1";
    public static final String USER_ID_1 = "1234";
    public static final String USER_ID_2 = "123";

    public static final String TOKEN_1 = "00164d1f7fd65332a0121ba65b3OwD+J58BZwHaZOc742QBADEEADEyMzQGAAAA5zvjZAEA5zvjZAIA5zvjZAMA5zvjZAQA5zvjZAUA5zvjZCAA4ZqE00jX0F42ej9WO9DY50OvI8OHYw12VJKfFUYtFLE=";
    public static final String TOKEN_2 = "00164d1f7fd65332a0121ba65b3OgA4oIsAUwHaZNM742QBADEDADEyMwYAAADTO+NkAQDTO+NkAgDTO+NkAwDTO+NkBADTO+NkBQDTO+NkIACPqSnRunGy8h7l6thvWGRQ/3ZH0fVzYBHn5+QCnmBJ9w==";

    //INPUT_REGEX SDK 对房间名、用户名的限制是：非空且最大长度不超过128位的数字、大小写字母、@ . _ -
    public static final String INPUT_REGEX = "^[a-zA-Z0-9@._-]{1,128}$";

    public static final String ROOM_ID_EXTRA = "extra_room_id";

    public static final String USER_ID_EXTRA = "extra_user_id";

    public static final String SDK_TOKEN = "token";
}
