package com.example.calcbattle.user

package object api {

  /**
    * ユーザー ID
    */
  case class UID(val underlying: String) extends AnyVal

  /**
    * ユーザーの状態が更新された
    * @param uid 状態が更新されたユーザーのID
    * @param continuationCurrent 現在の連続回答数
    */
  case class UserUpdated(uid: UID, continuationCurrent: Int)

  /**
    * ユーザーが連続回答のノルマを達成した
    * @param uid ノルマを達成したユーザーのID
    */
  case class UserCompleted(uid: UID)

  /**
    * ユーザーの回答
    * @param uid 回答したユーザーのID
    * @param questionA 問題の数字 A
    * @param questionB 問題の数字 B
    * @param userInput ユーザーの入力
    */
  case class Answer(uid: UID, questionA: Int, questionB: Int, userInput: Int) {

    def isCorrect = (questionA + questionB) == userInput
  }

  /**
    * ユーザーの回答結果
    * @param answerIsCorrect
    */
  case class Result(answerIsCorrect: Boolean)

}
