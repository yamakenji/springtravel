const stripe = Stripe('pk_test_51Lcdg9H3mwyalYUk46aziPeYiD1r5WzCPaNoo9R13g0s145ose5W8t3OGhxQc78e2K7fJDJ6jcY6LiToqO6dlpwn00wnPuGSnz');
const paymentButton = document.querySelector('#paymentButton');

paymentButton.addEventListener('click', () => {
  stripe.redirectToCheckout({
    sessionId: sessionId
  })
});