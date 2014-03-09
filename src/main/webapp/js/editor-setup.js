$(document).ready(function() {
  var editorInfoByFilename = {};

  $(document)
    .on('show-editor-for', function(_, filename) {
      var existingEditorInfo = editorInfoByFilename[filename];

      $('ul.tab-contents > .current, menu.tabs > .current')
        .removeClass('current');

      if (existingEditorInfo) {
        existingEditorInfo.$contents.add(existingEditorInfo.$tab)
          .addClass('current');
      } else {
        var $contents =
          $('<li>')
            .attr('class', 'tab-contents editor current loading')
            .append(
              $('<div></div>')
                .attr('class', 'editor')
            );
        var $tab =
          $('<li>')
            .attr('class', 'tab editor current loading')
            .append(
              $('<button>')
                .click(function() {
                  $(document).trigger('show-editor-for', filename);
                })
                .text(filename)
            )

        $('ul.tab-contents')
          .append($contents);
        $('menu.tabs')
          .append($tab);

        var editor = ace.edit($contents.find('div.editor')[0]);
        editor.setTheme('ace/theme/monokai');
        editor.getSession().setMode('ace/mode/scala');

        $(document).trigger('load-content-for-document', {
          filename: filename,
          onComplete: function(contents) {
            // change this to create the editor tab, etc
            editor.setValue(contents, -1);

            editorInfoByFilename[filename] = {
              editor: editor,
              $contents: $contents,
              $tab: $tab
            };

            $contents.add($tab).removeClass('loading');
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
    })
    .on('errors-cleared', function() {
      for (var filename in editorsByFilename) {
        editorsByFilename[filename].getSession().setAnnotations([]);
      }
    });
});
