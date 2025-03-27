let maxDate = new Date();
maxDate = maxDate.setMonth(maxDate.getMonth() + 3);

flatpickr('#fromCheckinDateToCheckoutDate', {
  mode: "range",
  locale: 'ja',
  minDate: 'today',
  maxDate: maxDate,
  onClose: function(selectedDates, dateStr, instance) {
    const dates = dateStr.split(" から ");
    if (dates.length === 2) {
      document.querySelector("input[name='checkinDate']").value = dates[0];
      document.querySelector("input[name='checkoutDate']").value = dates[1];
    } else {
      document.querySelector("input[name='checkinDate']").value = '';
      document.querySelector("input[name='checkoutDate']").value = '';
    }
  }
});