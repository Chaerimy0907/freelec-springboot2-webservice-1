package com.jojoldu.book.springboot.web;

// JUnit 5 관련 import
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jojoldu.book.springboot.web.dto.PostsUpdateRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// Sping Boot 3.2에서는 @ExtendWith(SpringExtension.class 불필요 - @SpringBootTest가 자동 처리

//Spring Boot 테스트 관련 import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort; // Spring Boot 3.x 용

// HTTP 관련 import
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// 도메인 관련 import (누락된 것들 추가)
import com.jojoldu.book.springboot.domain.posts.Posts;
import com.jojoldu.book.springboot.domain.posts.PostsRepository;
import com.jojoldu.book.springboot.web.dto.PostsSaveRequestDto;
import org.springframework.test.context.junit4.SpringRunner;

// 검증 라이브러리
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FrameworkServlet;

import static org.assertj.core.api.Assertions.setPrintAssertionsDescription;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Boot 3.2 통합 테스트
 *  - 실제 서버를 랜덤 포트에 띄워서 HTTP 요청/응답 테스트
 *  - JUnit 5 기반
 */

//@ExtendWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)  // 랜덤 포트로 실제 서버 시작
//@AutoConfigureMockMvc
public class PostsApiControllerTest {
    /**
     * 실제 서버가 시작된 포트 번호를 주입 받음
     * 예 : 8080이 사용 중이면 58423 같은 포트 사용
     */
    @LocalServerPort
    private int port;

    /**
     * 실제 HTTP 요청을 보내기 위한 클라이언트
     * RestTemplate의 테스트용 버전
     */
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * 데이터베이스 조작을 위한 Repository
     * 테스트 후 데이터 정리용으로 사용
     */
    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()).build();

//        context.getServletContext()
//                .setAttribute(FrameworkServlet.SERVLET_CONTEXT_PREFIX, context);
    }

    /**
     * 각 테스트 메소드 실행 후 호출되는 정리(cleanup) 메소드
     * 테스트 간 데이터 간섭을 방지하기 위해 DB 데이터 삭제
     */
    @AfterEach
    public void tearDown() throws Exception {
        postsRepository.deleteAll();  // 모든 게시글 데이터 삭제
    }

    /**
     * 게시글 등록 API 통합 테스트
     *  - 실제 HTTP POST 요청을 보내서 게시글 생성 확인
     *  - 응답 상태코드와 데이터베이스 저장 여부 검증
     */
    @Test
    @WithMockUser(roles="USER")
    public void Posts_등록된다() throws Exception {
        // given - 테스트에 필요한 데이터 준비
        String title = "title";
        String content = "content";

        // Builder 패턴으로 요청 DTO 생성
        PostsSaveRequestDto requestDto = PostsSaveRequestDto.builder()
                .title(title)
                .content(content)
                .author("author")
                .build();

        // 실제 요청할 URL 생성 (랜덤 포트 사용)
        //String url = "http://localhost:" + port + "/api/v1/posts";

        // when - 실제 테스트 실행
        // TestRestTemplate으로 실제 HTTP POST 요청 전송
        //ResponseEntity<Long> responseEntity = restTemplate.postForEntity(url, requestDto, Long.class);
        mvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        // then - 결과 검증
        // 1. HTTP 응답 상태코드가 200 OK인지 확인
        //assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. 응답 body에 생성된 게시글 ID가 0보다 큰지 확인 (자동 생성된 ID)
        //assertThat(responseEntity.getBody()).isGreaterThan(0L);

        // 3. 데이터베이스에 실제로 데이터가 저장되었는지 확인
        List<Posts> all = postsRepository.findAll();    // 모든 게시글 조회
        assertThat(all.get(0).getTitle()).isEqualTo(title);     // 제목 확인
        assertThat(all.get(0).getContent()).isEqualTo(content); // 내용 확인
    }

    @Test
    @WithMockUser(roles="USER")
    public void Posts_수정된다() throws Exception {
        // given
        Posts savedPosts = postsRepository.save(Posts.builder()
                .title("title")
                .content("content")
                .author("author")
                .build());

        Long updateId = savedPosts.getId();
        String expectedTitle = "title2";
        String expectedContent = "content2";

        PostsUpdateRequestDto requestDto = PostsUpdateRequestDto.builder()
                .title(expectedTitle)
                .content(expectedContent)
                .build();

        //String url = "http://localhost:" + port + "/api/v1/posts/" + updateId;

        //HttpEntity<PostsUpdateRequestDto> requestEntity = new HttpEntity<>(requestDto);

        // when
        //ResponseEntity<Long> responseEntity = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Long.class);
        mvc.perform(put("/api/v1/posts/{id}", updateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        // then
        List<Posts> all = postsRepository.findAll();
        assertThat(all.get(0).getTitle()).isEqualTo(expectedTitle);
        assertThat(all.get(0).getContent()).isEqualTo(expectedContent);
    }

}
