$(document).ready(function() {
  function compile() {
    Sbt.withSbt(function(sbt) {
      $('li.build-status')
        .removeClass('error');
      $('progress.build-status')
        .removeAttr('value');

      sbt.runCommand('compile');

      var errors = [];

      function compileComplete(_, sbtLine) {
        var match = sbtLine.match(/\[(success|error)\] Total time/)
        if (match) {
          $('progress.build-status').attr('value', '100')

          $(document).off('sbt-line-received.compile');

          if (match[1] == 'error') {
            $('li.build-status').addClass('error')
          } else {
            $(document).trigger('errors-cleared')
          }
        }
      }
      var currentError = null;
      function compileProgress(_, sbtLine) {
        var errorStartMatch = sbtLine.match(/\[error\] ([^:]+):(\d+): (.*)$/),
            errorEndMatch = sbtLine.match(/\[error\] (one|two|\d+) errors? found/),
            errorLineMatch = sbtLine.match(/\[error\] (.*)$/);
        if (errorStartMatch) {
          if (currentError) {
            errors.push(currentError);
          }

          var filename = errorStartMatch[1];

          currentError = {
            file: filename,
            row: parseInt(errorStartMatch[2]),
            text: errorStartMatch[3]
          };
        } else if (errorEndMatch) {
          if (currentError) {
            errors.push(currentError);
          }

          var errorsByFile = {};
          errors.forEach(function(error) {
            errorsByFile[error.file] = errorsByFile[error.file] || [];
            errorsByFile[error.file].push(error);
            delete error.file;
          });

          for (var file in errorsByFile) {
            $(document).trigger('errors-received', {
              filename: file,
              errors: errorsByFile[file]
            });
          }
        } else if (errorLineMatch && currentError) {
          currentError.text += "\n" + errorLineMatch[1];
        }
      }

      $(document)
        .on('sbt-line-received.compile', compileProgress)
        .on('sbt-line-received.compile', compileComplete);
    });
  }

  $(document).on('editor-contents-saved', compile)
});
