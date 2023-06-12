package com.co.kr.FileUpload;
import okhttp3.*;

import java.io.File;
import java.io.IOException;




public class FileUploader {
    private static final String SERVER_URL = "http://127.0.0.1:5000"; // Flask 서버의 업로드 URL
    static String getResult;
    static String [] getToken;
    static String className, calories;
//
//    public static void main(String[] args) {
//        File file = new File("C:/upload/14.jpg"); // 전송할 파일 경로
//
//        //스프링에서 이미지 보내는 주소 이걸 입력하면 되고
//        try {
//            getResult = uploadFile(file);
//            getToken = getResult.split("'");
//            className = getToken[3];
//            calories = getToken[7];
////            System.out.println(className + "\n" + calories);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
 static String uploadFile(File file) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String  result = null;

        // 멀티파트 요청 작성
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse("image/jpg")))
                .build();

        // POST 요청 생성 및 파일 이름 헤더 추가
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .header("FileName", file.getName())
                .post(requestBody)
                .build();

        // 요청 실행
        try (Response response = client.newCall(request).execute()) {
        	if (response.isSuccessful()) {
                System.out.println("파일 전송 성공");

//                System.out.println(response.body().string()); //여기서 값을 받아오는 부분이니
                result = String.valueOf(response.body().string());
                System.out.println(result);
                return result;
                
            } else {
                System.out.println("파일 전송 실패");
            }
        }
        return result;
    }
}