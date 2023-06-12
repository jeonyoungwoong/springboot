package com.co.kr.service;


import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.co.kr.domain.DocuList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.co.kr.code.Code;
import com.co.kr.domain.BoardContentDomain;
import com.co.kr.domain.BoardFileDomain;
import com.co.kr.domain.BoardListDomain;
import com.co.kr.exception.RequestException;
import com.co.kr.mapper.UploadMapper;
import com.co.kr.util.CommonUtils;
import com.co.kr.vo.FileListVO;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Service
@Transactional
public class UploadServiceImple implements UploadService {

	@Autowired
	private UploadMapper uploadMapper;
	
	@Override
	public List<BoardListDomain> boardList() {
		// TODO Auto-generated method stub
		return uploadMapper.boardList();
	}

	public List<DocuList> docuList(String mbId){
		return uploadMapper.docuList(mbId);
	}
	
	private BoardContentDomain setBoardCotentDomain(FileListVO fileListVO, HttpServletRequest httpReq) {
		HttpSession session = httpReq.getSession();
		BoardContentDomain boardContentDomain = BoardContentDomain.builder()
				.mbId(session.getAttribute("id").toString())
				.bdTitle(fileListVO.getTitle())
				.bdContent(fileListVO.getContent())
				.build();
		
		return boardContentDomain;
		
	}
	
	private String calorieChk(File file, String SERVER_URL) {
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
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return result;
    }
	
	private void modify(FileListVO fileListVO, MultipartHttpServletRequest request, BoardContentDomain boardContentDomain, HttpSession session, List<MultipartFile> multipartFiles) {
		String mbId = boardContentDomain.getMbId();
		
		//파일객체 담음
		// 게시글 수정시 파일관련 물리저장 파일, db 데이터 삭제 
		if(fileListVO.getIsEdit() != null) { // 수정시 
			List<BoardFileDomain> fileList = null;
			for (MultipartFile multipartFile : multipartFiles) {				
				if(!multipartFile.isEmpty()) {   // 수정시 새로 파일 첨부될때 세션에 담긴 파일 지우기
					if(session.getAttribute("files") != null) {	

						fileList = (List<BoardFileDomain>) session.getAttribute("files");
						
						for (BoardFileDomain list : fileList) {
							list.getUpFilePath();
							Path filePath = Paths.get(list.getUpFilePath());
					 
					        try {
					        	
					            // 파일 삭제
					            Files.deleteIfExists(filePath); // notfound시 exception 발생안하고 false 처리
					            //삭제 
								bdFileRemove(list); //데이터 삭제
								
					        } catch (DirectoryNotEmptyException e) {
								throw RequestException.fire(Code.E404, "디렉토리가 존재하지 않습니다", HttpStatus.NOT_FOUND);
					        } catch (IOException e) {
					            e.printStackTrace();
					        }
						}
					}
				}
			}
		}
	}
	
	private void insertDocument(BoardContentDomain boardContentDomain, List<MultipartFile> multipartFiles, int bdSeq, String serverUrl) {
		String mbId = boardContentDomain.getMbId();
		Path rootPath = Paths.get(new File("C://").toString(),"upload", File.separator).toAbsolutePath().normalize();			
		File pathCheck = new File(rootPath.toString());
	    String [] getToken;
	    String foodNm = "", calories ="0";
		
		// folder check
		if(!pathCheck.exists()) pathCheck.mkdirs();				

		for (MultipartFile multipartFile : multipartFiles) {
			
			if(!multipartFile.isEmpty()) {  // 파일 있을때 
				
				//확장자 추출
				String originalFileExtension;
				String contentType = multipartFile.getContentType();
				String origFilename = multipartFile.getOriginalFilename();
				
				//확장자 조재안을경우
				if(ObjectUtils.isEmpty(contentType)){
					break;
				}else { // 확장자가 jpeg, png인 파일들만 받아서 처리
					if(contentType.contains("image/jpeg")) {
						originalFileExtension = ".jpg";
					}else if(contentType.contains("image/png")) {
						originalFileExtension = ".png";
					}else {
						break;
					}
				}
				
				//파일명을 업로드한 날짜로 변환하여 저장
				String uuid = UUID.randomUUID().toString();
				String current = CommonUtils.currentTime();
				String newFileName = uuid + current + originalFileExtension;
				
				//최종경로까지 지정
				Path targetPath = rootPath.resolve(newFileName);
				
				File file = new File(targetPath.toString());
				
//				
				try {
					//파일복사저장
					multipartFile.transferTo(file);
					// 파일 권한 설정(쓰기, 읽기)
					file.setWritable(true);
					file.setReadable(true);
					getToken = calorieChk(file, serverUrl).split("'");
					foodNm = getToken[3];
					calories = getToken[7];
					
					System.out.println(foodNm + " " + calories);
					
					
					//파일 domain 생성 
					BoardFileDomain boardFileDomain = BoardFileDomain.builder()
							.bdSeq(bdSeq)
							.mbId(mbId)
							.upOriginalFileName(origFilename)
							.upNewFileName("resources/upload/"+newFileName) // WebConfig에 동적 이미지 폴더 생성 했기때문
							.upFilePath(targetPath.toString())
							.upFileSize((int)multipartFile.getSize())
							.foodNm(foodNm)
							.calories(calories)
							.build();
					
						// db 인서트
						uploadMapper.fileUpload(boardFileDomain);
						System.out.println("upload done");
					
				} catch (IOException e) {
					throw RequestException.fire(Code.E404, "잘못된 업로드 파일", HttpStatus.NOT_FOUND);
				}
			}

		}
	}

	@Override
	public int fileProcess(FileListVO fileListVO, MultipartHttpServletRequest request, HttpServletRequest httpReq) {
		final String serverUrl = "http://127.0.0.1:5000"; // Flask 서버의 업로드 URL
		//session 생성
		HttpSession session = httpReq.getSession();
		
		//content domain 생성 
		BoardContentDomain boardContentDomain = setBoardCotentDomain(fileListVO, httpReq);

		
		if(fileListVO.getIsEdit() != null) {
			boardContentDomain.setBdSeq(Integer.parseInt(fileListVO.getSeq()));
			System.out.println("수정업데이트");
			// db 업데이트
			uploadMapper.bdContentUpdate(boardContentDomain);
		}else {	
			// db 인서트
			uploadMapper.contentUpload(boardContentDomain);
			System.out.println(" db 인서트");
		}
				
				// file 데이터 db 저장시 쓰일 값 추출
		int bdSeq = boardContentDomain.getBdSeq();
		List<MultipartFile> multipartFiles = request.getFiles("files");
		
		modify(fileListVO, request, boardContentDomain, session, multipartFiles);
		insertDocument(boardContentDomain, multipartFiles, bdSeq, serverUrl);
		
		return bdSeq;
		
		
		
		///////////////////////////// 새로운 파일 저장 ///////////////////////
		
		// 저장 root 경로만들기
	}

	@Override
	public void bdContentRemove(HashMap<String, Object> map) {
		uploadMapper.bdContentRemove(map);
	}

	@Override
	public void bdFileRemove(BoardFileDomain boardFileDomain) {
		uploadMapper.bdFileRemove(boardFileDomain);
	}
	
	// 하나만 가져오기
	@Override
	public BoardListDomain boardSelectOne(HashMap<String, Object> map) {
		return uploadMapper.boardSelectOne(map);
	}

	// 하나 게시글 파일만 가져오기
	@Override
	public List<BoardFileDomain> boardSelectOneFile(HashMap<String, Object> map) {
		return uploadMapper.boardSelectOneFile(map);
	}

	
	
}