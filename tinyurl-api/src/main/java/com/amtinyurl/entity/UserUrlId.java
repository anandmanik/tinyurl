package com.amtinyurl.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUrlId implements Serializable {

    private String userIdLower;
    private String code;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserUrlId that = (UserUrlId) o;
        return Objects.equals(userIdLower, that.userIdLower) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userIdLower, code);
    }
}