package com.example.demo;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 이미 Bean Validation 을 쓰는 폼 (방식 2 — {@link ProfileController}).
 */
public class ProfileForm {
    @NotBlank
    @Size(max = 10)
    private String nickname;

    @NotNull
    @Min(14)
    @Max(120)
    private Integer age;

    @Pattern(regexp = "[0-9]{5}")
    private String zip;

    @Size(min = 1, max = 2)
    private List<String> tags;

    @Past
    private LocalDate birthDate; // 브라우저 규칙으로 바꾸지 않음(서버 전용)

    private String type;    // "VIP" 이면 vipCode 필수 — Bean Validation 에 없는 조건부 규칙은 S2 로
    private String vipCode;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getVipCode() { return vipCode; }
    public void setVipCode(String vipCode) { this.vipCode = vipCode; }
}
