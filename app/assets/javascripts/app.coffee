$ ->
  ws = new WebSocket $('body').data 'ws-url'
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    switch message.type
      when 'question'
        a = message.question.a
        b = message.question.b
        $('#question').html "<span id='question-a'>#{a}</span><span> + </span>><span'question-b'>#{b}</span>"
        $('#answer').attr 'answer', a + b
      when 'result'

      when 'updateUser'
        for uid, continuationCorrect of message.user
          $("#uid_#{uid}").empty()
          updateStar(uid, continuationCorrect)
          finishEffect(uid) if message.finish
      when 'updateUsers'
        $('#users').empty()
        for uid, continuationCorrect of message.users
          $('#users').append "<li id=\"uid_#{uid}\" class=\"list-group-item\"></li>"
          updateStar(uid, continuationCorrect)
      else
        console.log "[Error] unmatch message: #{message}"

  updateStar = (uid, continuationCorrect) ->
    $("#uid_#{uid}").append "ユーザ#{uid} "
    unless continuationCorrect is 0
      for i in [1..continuationCorrect]
        $("#uid_#{uid}").append "<span class=\"glyphicon glyphicon-star\" aria-hidden=\"true\"></span>"
    unless continuationCorrect is 5
      for i in [1..(5 - continuationCorrect)]
        $("#uid_#{uid}").append "<span class=\"glyphicon glyphicon-star-empty\" aria-hidden=\"true\"></span>"

  finishEffect = (uid) ->
    $('#answer').attr 'disabled', 'disabled'
    $("#uid_#{uid}").addClass 'list-group-item-success'

  $(document).on 'keypress', '#answer', (e) ->
    ENTER = 13
    if e.which is ENTER
      questionA = $("#question-a").text()
      questionB = $("#question-b").text()
      input = $(this).val().trim()
      return unless input
      # TODO: UID 足りへん
      ws.send JSON.stringify { answer: { questionA: questionA, questionB: questionB, userInput: input } }
      $(this).val ''

  $('#start').click ->
      ws.send JSON.stringify { start: true }
