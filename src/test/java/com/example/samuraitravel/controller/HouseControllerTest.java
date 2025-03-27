package com.example.samuraitravel.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class HouseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void 未ログインの場合は会員用の民宿一覧ページが正しく表示される() throws Exception {
        mockMvc.perform(get("/houses"))
               .andExpect(status().isOk())
               .andExpect(view().name("houses/index"));
    }

    @Test
    @WithUserDetails("taro.samurai@example.com")
    public void ログイン済みの場合は会員用の民宿一覧ページが正しく表示される() throws Exception {
        mockMvc.perform(get("/houses"))
               .andExpect(status().isOk())
               .andExpect(view().name("houses/index"));
    }

    @Test
    public void 未ログインの場合は会員用の民宿詳細ページが正しく表示される() throws Exception {
        mockMvc.perform(get("/houses/1"))
               .andExpect(status().isOk())
               .andExpect(view().name("houses/show"));
    }

    @Test
    @WithUserDetails("taro.samurai@example.com")
    public void ログイン済みの場合は会員用の民宿詳細ページが正しく表示される() throws Exception {
        mockMvc.perform(get("/houses/1"))
               .andExpect(status().isOk())
               .andExpect(view().name("houses/show"));
    }
}
