package com.example.samuraitravel.service;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.samuraitravel.dto.ReservationDTO;
import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.repository.HouseRepository;
import com.stripe.Stripe;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.ApiException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.PermissionException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.Mode;
import com.stripe.param.checkout.SessionCreateParams.PaymentMethodType;
import com.stripe.param.checkout.SessionRetrieveParams;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;

@Service
public class StripeService {
    // 定数
    private static final PaymentMethodType PAYMENT_METHOD_TYPE = SessionCreateParams.PaymentMethodType.CARD;  // 決済方法
    private static final String CURRENCY = "jpy";  // 通貨
    private static final long QUANTITY = 1L;  // 数量
    private static final Mode MODE = SessionCreateParams.Mode.PAYMENT;  // 支払いモード
    private static final DateTimeFormatter DATE_TIME_FORMATTER  = DateTimeFormatter.ofPattern("yyyy-MM-dd");  // 日付のフォーマット

    // Stripeのシークレットキー
    @Value("${stripe.api-key}")
    private String stripeApiKey;

    // 決済成功時のリダイレクト先URL
    @Value("${stripe.success-url}")
    private String stripeSuccessUrl;

    // 決済キャンセル時のリダイレクト先URL
    @Value("${stripe.cancel-url}")
    private String stripeCancelUrl;

    private final HouseRepository houseRepository;
    private final ReservationService reservationService;

    public StripeService(HouseRepository houseRepository, ReservationService reservationService) {
        this.houseRepository = houseRepository;
        this.reservationService = reservationService;
    }

    // 依存性の注入後に一度だけ実行するメソッド
    @PostConstruct
    private void init() {
        // Stripeのシークレットキーを設定する
        Stripe.apiKey = stripeApiKey;
    }

    public String createStripeSession(ReservationDTO reservationDTO, User user) {
        Optional<House> optionalHouse = houseRepository.findById(reservationDTO.getHouseId());
        House house = optionalHouse.orElseThrow(() -> new EntityNotFoundException("指定されたIDの民宿が存在しません。"));

        // 商品名
        String houseName = house.getName();

        // 料金
        long unitAmount = (long)reservationDTO.getAmount();

        // メタデータ（付随情報）
        String houseId = reservationDTO.getHouseId().toString();
        String userId = user.getId().toString();
        String checkinDate = reservationDTO.getCheckinDate().format(DATE_TIME_FORMATTER);
        String checkoutDate = reservationDTO.getCheckoutDate().format(DATE_TIME_FORMATTER);
        String numberOfPeople = reservationDTO.getNumberOfPeople().toString();
        String amount = reservationDTO.getAmount().toString();

        // セッションに入れる支払い情報
        SessionCreateParams sessionCreateParams =
            SessionCreateParams.builder()
                .addPaymentMethodType(PAYMENT_METHOD_TYPE)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(houseName)
                                        .build())
                                .setUnitAmount(unitAmount)
                                .setCurrency(CURRENCY)
                                .build())
                        .setQuantity(QUANTITY)
                        .build())
                .setMode(MODE)
                .setSuccessUrl(stripeSuccessUrl)
                .setCancelUrl(stripeCancelUrl)
                .setPaymentIntentData(
                    SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("houseId", houseId)
                        .putMetadata("userId", userId)
                        .putMetadata("checkinDate", checkinDate)
                        .putMetadata("checkoutDate", checkoutDate)
                        .putMetadata("numberOfPeople", numberOfPeople)
                        .putMetadata("amount", amount)
                        .build())
                .build();

        try {
            // Stripeに送信する支払い情報をセッションとして作成する
            Session session = Session.create(sessionCreateParams);

            // 作成したセッションのIDを返す
            return session.getId();
        } catch (RateLimitException e) {
            System.out.println("短時間のうちに過剰な回数のAPIコールが行われました。");
            return "";
        } catch (InvalidRequestException e) {
            System.out.println("APIコールのパラメーターが誤っているか、状態が誤っているか、方法が無効でした。");
            return "";
        } catch (PermissionException e) {
            System.out.println("このリクエストに使用されたAPIキーには必要な権限がありません。");
            return "";
        } catch (AuthenticationException e) {
            System.out.println("Stripeは、提供された情報では認証できません。");
            return "";
        } catch (ApiConnectionException e) {
            System.out.println("お客様のサーバーとStripeの間でネットワークの問題が発生しました。");
            return "";
        } catch (ApiException e) {
            System.out.println("Stripe側で問題が発生しました（稀な状況です）。");
            return "";
        } catch (StripeException e) {
            System.out.println("Stripeとの通信中に予期せぬエラーが発生しました。");
            return "";
        }
    }

    // セッションから予約情報を取得し、ReservationServiceクラスを介してデータベースに登録する
    public void processSessionCompleted(Event event) {
        // EventオブジェクトからStripeObjectオブジェクトを取得する
        Optional<StripeObject> optionalStripeObject = event.getDataObjectDeserializer().getObject();

        optionalStripeObject.ifPresentOrElse(stripeObject -> {
            // StripeObjectオブジェクトをSessionオブジェクトに型変換する
            Session session = (Session)stripeObject;

            // "payment_intent"情報を展開する（詳細情報を含める）ように指定したSessionRetrieveParamsオブジェクトを生成する
            SessionRetrieveParams sessionRetrieveParams = SessionRetrieveParams.builder().addExpand("payment_intent").build();

            try {
                // 支払い情報を含む詳細なセッション情報を取得する
                session = Session.retrieve(session.getId(), sessionRetrieveParams, null);

                // 詳細なセッション情報からメタデータ（予約情報）を取り出す
                Map<String, String> sessionMetadata = session.getPaymentIntentObject().getMetadata();

                // 予約情報をデータベースに登録する
                reservationService.createReservation(sessionMetadata);

                System.out.println("予約情報の登録処理が成功しました。");

            } catch (RateLimitException e) {
                System.out.println("短時間のうちに過剰な回数のAPIコールが行われました。");
            } catch (InvalidRequestException e) {
                System.out.println("APIコールのパラメーターが誤っているか、状態が誤っているか、方法が無効でした。");
            } catch (PermissionException e) {
                System.out.println("このリクエストに使用されたAPIキーには必要な権限がありません。");
            } catch (AuthenticationException e) {
                System.out.println("Stripeは、提供された情報では認証できません。");
            } catch (ApiConnectionException e) {
                System.out.println("お客様のサーバーとStripeの間でネットワークの問題が発生しました。");
            } catch (ApiException e) {
                System.out.println("Stripe側で問題が発生しました（稀な状況です）。");
            } catch (StripeException e) {
                System.out.println("Stripeとの通信中に予期せぬエラーが発生しました。");
            } catch (Exception e) {
                System.out.println("予約情報の登録処理中に予期せぬエラーが発生しました。");
            }
        },
        () -> {
            System.out.println("予約情報の登録処理が失敗しました。");
        });

        // StripeのAPIとstripe-javaライブラリのバージョンをコンソールに出力する
        System.out.println("Stripe API Version: " + event.getApiVersion());
        System.out.println("stripe-java Version: " + Stripe.VERSION + ", stripe-java API Version: " + Stripe.API_VERSION);
    }
}
