$(document).ready(function() {
  $('#files').on('click', 'a[href]', function() {
    $(document).trigger('show-editor-for', this.getAttribute('href'));

    return false;
  })

  $('button.save').on('click', function() {
    $(document).trigger('save-editor-contents');
  })
});
