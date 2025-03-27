package com.example.samuraitravel.form;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationInputForm {
    @NotNull(message = "チェックイン日を選択してください。")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkinDate;

    @NotNull(message = "チェックアウト日を選択してください。")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkoutDate;

    @NotNull(message = "宿泊人数を入力してください。")
    @Min(value = 1, message = "宿泊人数は1人以上に設定してください。")
    private Integer numberOfPeople;
}
