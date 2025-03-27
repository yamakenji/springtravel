package com.example.samuraitravel.controller;

// import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.transaction.annotation.Transactional;

import com.example.samuraitravel.dto.ReservationDTO;
// import com.example.samuraitravel.entity.Reservation;
// import com.example.samuraitravel.service.ReservationService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ReservationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    /*
    @Autowired
    private ReservationService reservationService;
    */

    @Test
    public void 未ログインの場合は会員用の予約一覧ページからログインページにリダイレクトする() throws Exception {
        mockMvc.perform(get("/reservations"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithUserDetails("taro.samurai@example.com")
    public void ログイン済みの場合は会員用の予約一覧ページが正しく表示される() throws Exception {
        mockMvc.perform(get("/reservations"))
               .andExpect(status().isOk())
               .andExpect(view().name("reservations/index"));
    }

    @Test
    public void 未ログインの場合は予約フォームを送信せずにログインページにリダイレクトする() throws Exception {
        mockMvc.perform(post("/houses/1/reservations/input").with(csrf())
                .param("checkinDate", "2024-04-01")
                .param("checkoutDate", "2024-04-02")
                .param("numberOfPeople", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithUserDetails("taro.samurai@example.com")
    public void ログイン済みの場合は予約フォームの送信後に予約内容の確認ページにリダイレクトする() throws Exception {
        mockMvc.perform(post("/houses/1/reservations/input").with(csrf())
                .param("checkinDate", "2024-04-01")
                .param("checkoutDate", "2024-04-02")
                .param("numberOfPeople", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/reservations/confirm"));
    }

    @Test
    public void 未ログインの場合は予約内容の確認ページからログインページにリダイレクトする() throws Exception {
        // セッションを作成し、ReservationDTOオブジェクトを保存する
        MockHttpSession mockHttpSession = new MockHttpSession();
        ReservationDTO reservationDTO = new ReservationDTO(1, LocalDate.parse("2024-04-01"), LocalDate.parse("2024-04-02"), 1, 6000);
        mockHttpSession.setAttribute("reservationDTO", reservationDTO);

        // 作成したセッションを使用してHTTPリクエストを送信する
        mockMvc.perform(get("/reservations/confirm").session(mockHttpSession))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithUserDetails("taro.samurai@example.com")
    public void ログイン済みの場合は予約内容の確認ページが正しく表示される() throws Exception {
        // セッションを作成し、ReservationDTOオブジェクトを保存する
        MockHttpSession mockHttpSession = new MockHttpSession();
        ReservationDTO reservationDTO = new ReservationDTO(1, LocalDate.parse("2024-04-01"), LocalDate.parse("2024-04-02"), 1, 6000);
        mockHttpSession.setAttribute("reservationDTO", reservationDTO);

        // 作成したセッションを使用してHTTPリクエストを送信する
        mockMvc.perform(get("/reservations/confirm").session(mockHttpSession))
               .andExpect(status().isOk())
               .andExpect(view().name("reservations/confirm"));
    }

    /*
    @Test
    @Transactional
    public void 未ログインの場合は予約せずにログインページにリダイレクトする() throws Exception {
        // テスト前のレコード数を取得する
        long countBefore = reservationService.countReservations();

        // セッションを作成し、ReservationDTOオブジェクトを保存する
        MockHttpSession mockHttpSession = new MockHttpSession();
        ReservationDTO reservationDTO = new ReservationDTO(1, LocalDate.parse("2024-04-01"), LocalDate.parse("2024-04-02"), 1, 6000);
        mockHttpSession.setAttribute("reservationDTO", reservationDTO);

        // 作成したセッションを使用してHTTPリクエストを送信する
        mockMvc.perform(post("/reservations/create").session(mockHttpSession).with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("http://localhost/login"));

        // テスト後のレコード数を取得する
        long countAfter = reservationService.countReservations();

        // レコード数が変わっていないことを検証する
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @WithUserDetails("taro.samurai@example.com")
    @Transactional
    public void ログイン済みの場合は予約後に会員用の予約一覧ページにリダイレクトする() throws Exception {
        // テスト前のレコード数を取得する
        long countBefore = reservationService.countReservations();

        // セッションを作成し、ReservationDTOオブジェクトを保存する
        MockHttpSession mockHttpSession = new MockHttpSession();
        ReservationDTO reservationDTO = new ReservationDTO(1, LocalDate.parse("2024-04-01"), LocalDate.parse("2024-04-02"), 1, 6000);
        mockHttpSession.setAttribute("reservationDTO", reservationDTO);

        // 作成したセッションを使用してHTTPリクエストを送信する
        mockMvc.perform(post("/reservations/create").session(mockHttpSession).with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/reservations?reserved"));

        // テスト後のレコード数を取得する
        long countAfter = reservationService.countReservations();

        // レコード数が1つ増加していることを検証する
        assertThat(countAfter).isEqualTo(countBefore + 1);

        Reservation reservation = reservationService.findFirstReservationByOrderByIdDesc();
        assertThat(reservation.getHouse().getId()).isEqualTo(1);
        assertThat(reservation.getUser().getEmail()).isEqualTo("taro.samurai@example.com");
        assertThat(reservation.getCheckinDate()).isEqualTo("2024-04-01");
        assertThat(reservation.getCheckoutDate()).isEqualTo("2024-04-02");
        assertThat(reservation.getNumberOfPeople()).isEqualTo(1);
        assertThat(reservation.getAmount()).isEqualTo(6000);
    }
    */
}
