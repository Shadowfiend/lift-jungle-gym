$(document).ready(function() {
  liftAjax.event = function(name, data) {
    var event, prop;

    event = $.Event(name);
    for (prop in data) {
      event[prop] = data[prop];
    }

    $(document).trigger(event);
  };
});
