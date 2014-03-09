$(document).ready(function() {
  var pendingDocumentLoads = {};
  var pendingDocumentSaves = {};

  var fileApiPrefix = $('body').data('file-api-prefix');

  function doSave(documentInfo) {
    $.ajax(documentInfo.filename,
      {
        method: 'POST',
        data: documentInfo.content,
        contentType: 'text/plain; charset=UTF-8'
      })
      .done(function() {
        documentInfo.onComplete();
      })
      .fail(function(_, status, error) {
        documentInfo.onComplete(status, error);
      })
      .always(function() {
        pendingDocumentSaves[documentInfo.filename].shift();

        if (pendingDocumentSaves[documentInfo.filename].length) {
          doSave(pendingDocumentSaves[documentInfo.filename][0]);
        }
      })
  }

  $(document)
    .on('load-content-for-document', function(_, documentInfo) {
      function clearPendingLoad() {
        delete pendingDocumentLoads[documentInfo.filename];
      }
      function reportFailure(_, status, error) {
        documentInfo.onComplete(null, status, error);
      }

      var pendingLoad = pendingDocumentLoads[documentInfo.filename];

      if (! pendingLoad) {
        pendingLoad =
          $.get(fileApiPrefix + documentInfo.filename)
            .always(clearPendingLoad)

        pendingDocumentLoads[documentInfo.filename] = pendingLoad;
      }

      pendingLoad
        .done(documentInfo.onComplete)
        .fail(reportFailure);
    })
    .on('save-content-for-document', function(_, documentInfo) {
      var pendingSaves = pendingDocumentSaves[documentInfo.filename] || (pendingDocumentSaves[documentInfo.filename] = []);
      pendingSaves.push(documentInfo);

      if (pendingSaves.length == 1) {
        // we are the only pending save, so we kick off the process
        doSave(documentInfo);
      }
    })
})
