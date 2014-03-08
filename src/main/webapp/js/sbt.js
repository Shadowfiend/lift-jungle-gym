(function() {
  var sbtConnecting = false,
      sbtConnected = false,
      whenConnectedCallbacks = [];

  var Sbt = {
    withSbt: function(whenConnected) {
      if (sbtConnected) {
        whenConnected(sbtRunner);
      } else {
        whenConnectedCallbacks.push(whenConnected);

        if (! sbtConnecting) {
          this.connectSbt();
        }
      }
    },
    connectSbt: function() {
      if (! sbtConnected && ! sbtConnecting) {
        $(document).one('sbt-line-received', function() {
          sbtConnecting = false;
          sbtConnected = true;

          whenConnectedCallbacks.forEach(function(fn) { fn(sbtRunner) });
        })

        sbtRunner.startSbt("").then(function(line) {
          $(document).trigger('sbt-line-received', line);
        });

        sbtConnecting = true;
      }
    }
  }

  window.Sbt = Sbt;

  $(document).ready(function() {
    if ($('body[data-debug]').length) {
      $(document).on('sbt-line-received', function(_, line) {
        console.log(line);
      });
    }
  });
})();
