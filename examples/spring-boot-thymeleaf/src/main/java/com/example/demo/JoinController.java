package com.example.demo;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import io.github.devers2.s2util.validation.S2RuleType;
import io.github.devers2.s2util.validation.S2ValidationResult;
import io.github.devers2.s2util.validation.S2Validator;
import io.github.devers2.s2util.validation.spring.S2BindValidator;

/**
 * 방식 1 — 규칙을 S2 로 한 번 정의해 화면(GET: 규칙 JSON)과 서버(POST: BindingResult)가 같은 규칙을 쓴다.
 */
@Controller
public class JoinController {

    /** 화면과 서버가 함께 쓰는 규칙 */
    static final S2Validator<JoinForm> RULES = S2Validator.<JoinForm>builder()
            .field("name", "이름").rule(S2RuleType.REQUIRED).rule(S2RuleType.MAX_LENGTH, 20)
            .field("email", "이메일").rule(S2RuleType.REQUIRED).rule(S2RuleType.EMAIL)
            .field("mobile", "휴대폰 번호").rule(S2RuleType.REQUIRED).rule(S2RuleType.MPHONE_NO)
            .field("password", "비밀번호").rule(S2RuleType.REQUIRED).rule(S2RuleType.PASSWORD)
            .field("passwordConfirm", "비밀번호 확인").rule(S2RuleType.REQUIRED).rule(S2RuleType.EQUALS_FIELD, "password")
            .field("memberType", "회원 유형").rule(S2RuleType.REQUIRED)
            .field("bizNo", "사업자등록번호").when("memberType", "BUSINESS")
                    .rule(S2RuleType.REQUIRED).rule(S2RuleType.BIZRNO, true)
            .field("interests", "관심사").rule(S2RuleType.MIN_SIZE, 1).rule(S2RuleType.MAX_SIZE, 3)
            .field("agree", "이용약관 동의").rule(S2RuleType.ASSERT_TRUE)
            .build();

    private final S2BindValidator.BoundContext<JoinForm> join = S2BindValidator.bind(RULES);

    @GetMapping({ "/", "/join" })
    public String form(Model model) {
        model.addAttribute("joinForm", new JoinForm());
        model.addAttribute("rules", join.getRulesJson()); // 요청 로케일의 메시지
        return "join";
    }

    @PostMapping("/join")
    public String submit(@ModelAttribute JoinForm joinForm, BindingResult result, Model model) {
        join.validate(joinForm, result); // 브라우저를 거치지 않은 요청도 서버가 같은 규칙으로 검증
        if (result.hasErrors()) {
            model.addAttribute("rules", join.getRulesJson());
            return "join";
        }
        model.addAttribute("message", joinForm.getName() + "님, 가입되었습니다.");
        return "done";
    }

    /** REST: 모든 오류를 브라우저와 같은 {필드: [메시지]} 형식으로 */
    @PostMapping("/api/join")
    @ResponseBody
    public ResponseEntity<Map<String, ?>> api(@RequestBody JoinForm joinForm, java.util.Locale locale) {
        S2ValidationResult result = RULES.validateAll(joinForm, locale);
        return result.isValid() ? ResponseEntity.ok(Map.of("ok", true)) : ResponseEntity.badRequest().body(result.toMap());
    }
}
