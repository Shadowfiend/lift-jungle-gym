$(document).ready(function() {
  $(document)
    .on('preview-launch-failed', function(event) {
      alert("Failed to launch preview; got " + event.error);
    })
    .on('preview-launched', function(event) {
      window.open("http://" + event.previewHost)
    })
});
