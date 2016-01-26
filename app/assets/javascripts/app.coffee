$ ->
  ws = new WebSocket $('body').data 'ws-url'
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    switch message.type
      when 'question'
        a = message.question.a
        b = message.question.b
        $('#question').html "<span id='question-a'>#{a}</span><span> + </span><span id='question-b'>#{b}</span>"
        $('#answer').attr 'answer', a + b
      when 'updateUser'
        user = message.user
        $("#uid_#{user.uid}").empty()
        updateStar(user.uid, user.continuationCorrect)
        finishEffect(user.uid) if message.finish
      when 'updateUsers'
        $('#users').empty()
        for user, index in message.users
          $('#users').append "<li id=\"uid_#{user.uid}\" class=\"list-group-item\"></li>"
          updateStar(user.uid, user.continuationCorrect)
      else
        console.log "[Error] unmatch message: #{message}"

  updateStar = (uid, continuationCorrect) ->
    $("#uid_#{uid}").append "<div>#{uid}</div>"
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
      questionA = parseInt($("#question-a").text())
      questionB = parseInt($("#question-b").text())
      input = parseInt($(this).val().trim())
      return unless input
      ws.send JSON.stringify { answer: { questionA: questionA, questionB: questionB, userInput: input } }
      $(this).val ''

  $('#start').click ->
      ws.send JSON.stringify { start: true }
