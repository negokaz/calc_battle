package com.example.calcbattle.user

package object api {

  /**
    * ユーザー ID
    */
  case class UID(val underlying: String) extends AnyVal

  /**
    * ユーザーとして参加する
    */
  case class Join(uid: UID)

  /**
    * ユーザーの状態をリクエストする
    */
  case class GetState(uid: UID)

  /**
    * ユーザーの状態
 *
    * @param uid 状態が更新されたユーザーのID
    * @param continuationCurrent 現在の連続回答数
    */
  case class UserState(uid: UID, continuationCurrent: Int) {

    /**
      * @return true: ユーザーが連続回答のノルマを達成した
      */
    def isCompleted = continuationCurrent >= 5
  }

  /**
    * ユーザーの状態が更新された
 *
    * @param user 更新されたユーザーの状態
    */
  case class UserUpdated(user: UserState)

  /**
    * ゲームに参加しているメンバの状態が変わった
    * (新しいメンバが参加, メンバが脱退 etc...)
 *
    * @param member 全ユーザーの状態
    */
  case class MemberUpdated(member: Set[UID])

  /**
    * ユーザーの回答
 *
    * @param questionA 問題の数字 A
    * @param questionB 問題の数字 B
    * @param userInput ユーザーの入力
    */
  case class Answer(uid: UID, questionA: Int, questionB: Int, userInput: Int) {

    def isCorrect = (questionA + questionB) == userInput
  }

  /**
    * ユーザーの回答結果
 *
    * @param answerIsCorrect
    */
  case class Result(uid: UID, answerIsCorrect: Boolean)

}
