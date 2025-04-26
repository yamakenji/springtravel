以下が予約からStripe決済までの流れと、各コンポーネントでのデータ処理の概要です。

# 予約(Reservation)からStripeでの決済処理の流れ

## 1. 予約入力フォームからの情報取得

### コンポーネント: `ReservationController#input`
- ユーザーが民宿詳細ページから予約情報を入力
- `ReservationInputForm`オブジェクトでフォームデータを受け取る
- 入力値のバリデーションを実施（チェックイン日がチェックアウト日より前か、人数が定員内か等）
- 宿泊料金を計算し、`ReservationDTO`オブジェクトを作成
- セッションに`ReservationDTO`を保存
- 予約確認画面へリダイレクト

```java
@PostMapping("/houses/{id}/reservations/input")
public String input(@PathVariable(name = "id") Integer id,
                    @ModelAttribute @Validated ReservationInputForm reservationInputForm,
                    /* ... */)
{
    // バリデーション処理
    
    // 宿泊料金計算
    Integer amount = reservationService.calculateAmount(checkinDate, checkoutDate, price);
    
    // DTOを作成しセッションに保存
    ReservationDTO reservationDTO = new ReservationDTO(house.getId(), checkinDate, checkoutDate, numberOfPeople, amount);
    httpSession.setAttribute("reservationDTO", reservationDTO);
    
    return "redirect:/reservations/confirm";
}
```

## 2. 予約確認と決済セッション作成

### コンポーネント: `ReservationController#confirm`
- セッションから`ReservationDTO`を取得
- ログインユーザー情報を取得
- `StripeService`を使用してStripeの決済セッションを作成
- 確認画面に`ReservationDTO`と`sessionId`を渡す

```java
@GetMapping("/reservations/confirm")
public String confirm(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
                      /* ... */) {
    // セッションからDTOを取得
    ReservationDTO reservationDTO = (ReservationDTO)httpSession.getAttribute("reservationDTO");
    
    User user = userDetailsImpl.getUser();
    
    // Stripe決済セッションを作成
    String sessionId = stripeService.createStripeSession(reservationDTO, user);
    
    model.addAttribute("reservationDTO", reservationDTO);
    model.addAttribute("sessionId", sessionId);
    
    return "reservations/confirm";
}
```

## 3. Stripe決済セッション作成

### コンポーネント: `StripeService#createStripeSession`
- `ReservationDTO`と`User`情報を受け取る
- `HouseRepository`から民宿情報を取得
- Stripeセッション作成用のパラメータを設定
  - 商品名、価格、通貨情報
  - 決済成功/キャンセル時のURL
  - 予約情報をメタデータとして保存（民宿ID、ユーザーID、チェックイン日、チェックアウト日等）
- Stripeセッションを作成し、セッションIDを返却

```java
public String createStripeSession(ReservationDTO reservationDTO, User user) {
    // 民宿情報取得
    House house = houseRepository.findById(reservationDTO.getHouseId())
                                 .orElseThrow(() -> new EntityNotFoundException("..."));
    
    // セッションパラメータ設定
    SessionCreateParams sessionCreateParams = SessionCreateParams.builder()
        .addPaymentMethodType(PAYMENT_METHOD_TYPE)
        .addLineItem(
            /* 商品情報・価格設定 */
        )
        .setMode(MODE)
        .setSuccessUrl(stripeSuccessUrl)
        .setCancelUrl(stripeCancelUrl)
        .setPaymentIntentData(
            /* メタデータ設定 */
        )
        .build();
        
    // Stripeセッション作成
    Session session = Session.create(sessionCreateParams);
    return session.getId();
}
```

## 4. フロントエンドでの決済処理

### コンポーネント: confirm.html と stripe.js
- confirm.htmlでStripe.jsライブラリを読み込み
- セッションIDをJavaScriptに渡す
- 「決済する」ボタンのクリックイベントを設定
- クリック時にStripeの決済画面にリダイレクト

```html
<!-- confirm.html -->
<script src="https://js.stripe.com/v3"></script>
<script th:inline="javascript">
   const sessionId = /*[[${sessionId}]]*/"sessionId";
</script>
<script th:src="@{/js/stripe.js}"></script>
```

```javascript
// stripe.js
const stripe = Stripe('pk_test_XXXXXXXXXX'); // 公開キー
const paymentButton = document.querySelector('#paymentButton');

paymentButton.addEventListener('click', () => {
  stripe.redirectToCheckout({
    sessionId: sessionId
  });
});
```

## 5. 決済完了後の処理

### コンポーネント: ウェブフック or `ReservationController`
- 決済成功時にStripeからのコールバックを処理
- セッションに保存された`ReservationDTO`を使用して予約エンティティを作成
- データベースに予約情報を保存
- 予約完了画面を表示

```java
@PostMapping("reservations/create")
public String create(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, /* ... */) {
    // セッションからDTOを取得
    ReservationDTO reservationDTO = (ReservationDTO)httpSession.getAttribute("reservationDTO");
    
    User user = userDetailsImpl.getUser();
    
    // 予約を作成
    reservationService.createReservation(reservationDTO, user);
    
    // セッションからDTOを削除
    httpSession.removeAttribute("reservationDTO");
    
    return "redirect:/reservations?reserved";
}
```

## データの流れ概要

1. `ReservationInputForm` → バリデーション → `ReservationDTO` (セッションに保存)
2. `ReservationDTO` + `User` → `StripeService` → `sessionId`
3. `sessionId` → フロントエンド (stripe.js) → Stripe決済ページ
4. 決済成功 → コールバック → `ReservationDTO` → `Reservation`エンティティ → データベース保存

