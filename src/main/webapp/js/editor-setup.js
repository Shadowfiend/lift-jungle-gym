$(document).ready(function() {
  $('#editor').append($('<div></div>').attr('class', 'editor'))

  var editor = ace.edit(document.querySelector('#editor > .editor'));
  editor.setTheme("ace/theme/monokai");
  editor.getSession().setMode("ace/mode/scala");

  var editorsByFilename = {}

  $(document)
    .on('show-editor-for', function(_, filename) {
      if (editorsByFilename[filename]) {
        // focus editor for this filename
      } else {
        $(document).trigger('load-content-for-document', {
          filename: filename,
          onComplete: function(contents) {
            // change this to create the editor tab, etc
            editor.setValue(contents, -1);

            editorsByFilename[filename] = editor;
           }
        })
      }
    })
    .on('save-editor-contents', function() {
      var pendingSaves = 0;
      function fileSaved() {
        pendingSaves--;
        if (pendingSaves === 0) {
          $(document).trigger('editor-contents-saved');
        }
      }
      for (filename in editorsByFilename) {
        pendingSaves++;

        $(document).trigger('save-content-for-document', {
          filename: filename,
          content: editorsByFilename[filename].getValue(),
          onComplete: fileSaved
        });
      }
    })
    .on('errors-received', function(_, errorInfo) {
      var errors = errorInfo.errors,
          filename = errorInfo.filename;

      var editor = editorsByFilename['/source/snippet/Editor.scala'];
      console.log('got', filename, errors, editor, editorsByFilename)
      if (editor) {
        editor.getSession().setAnnotations(
          errors.map(function(error) {
            return {
              type: 'error',
              text: error.text,
              row: error.row - 1,
              column: 0
            };
          })
        );
      }
    });
});
