package com.example.demo;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 예제가 실제로 동작하는지 확인: 화면에 규칙 JSON, 서버 검증, Bean Validation 과 S2 오류 병합, REST 오류 형식.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void joinPageCarriesTheRulesJson() throws Exception {
        mvc.perform(get("/join").locale(Locale.KOREAN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-s2-rules=")))
                .andExpect(content().string(containsString("이름은 필수 입력 항목입니다.")))
                .andExpect(content().string(containsString("data-s2-error-for=\"name\""))); // 브라우저 오류 표시 자리
    }

    @Test
    void serverValidatesWithTheSameRules() throws Exception {
        mvc.perform(post("/join").locale(Locale.KOREAN).param("memberType", "BUSINESS").param("bizNo", "123-45-67890"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("joinForm", "name", "bizNo", "agree"));
    }

    @Test
    void beanValidationAndS2ErrorsTogether() throws Exception {
        mvc.perform(post("/profile").locale(Locale.KOREAN).param("type", "VIP").param("age", "20"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("profileForm", "nickname", "vipCode"));
    }

    @Test
    void profilePageRulesComeFromAnnotations() throws Exception {
        mvc.perform(get("/profile").locale(Locale.KOREAN))
                .andExpect(content().string(containsString("닉네임은 필수 입력 항목입니다.")))
                .andExpect(content().string(containsString("MAX_LENGTH")));
    }

    @Test
    void restReturnsAllErrorsInTheBrowserShape() throws Exception {
        mvc.perform(post("/api/join").locale(Locale.KOREAN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"홍길동\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email[0]").value("이메일 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.name").doesNotExist());
    }
}
