package com.adityaamolbavadekar.waveassistant

import android.graphics.drawable.Drawable
import com.adityaamolbavadekar.waveassistant.Assistant.Actions.Companion.ACTION_NONE

data class QueryResult(
    var rootQueryText: String,
    var action: String,
    var answerText: String,
    var answerMessages: List<String>? = null,
    var photo: Drawable? = null,
    var queryLabelType: TextQuery.LabelType
) {
    class Builder {

        private var _rootQueryText = ""
        private var _action = ACTION_NONE
        private var _answerText = ""
        private var _answers: List<String> = listOf()
        private var _photo: Drawable? = null
        private var _queryLabelType: TextQuery.LabelType = TextQuery.LabelType.UNKNOWN

        fun withRootQuery(s: String): Builder {
            this._rootQueryText = s
            return this
        }

        fun withAction(assistantAction: String): Builder {
            this._action = assistantAction
            return this
        }

        fun withPreferredAnswer(s: String): Builder {
            this._answerText = s
            return this
        }

        fun withAnswersList(s: List<String>): Builder {
            this._answers = s
            return this
        }

        fun withPhoto(s: Drawable?): Builder {
            this._photo = s
            return this
        }

        fun withLabelType(s: TextQuery.LabelType): Builder {
            this._queryLabelType = s
            return this
        }

        fun build(): QueryResult {
            return QueryResult(
                rootQueryText = _rootQueryText,
                action = _action,
                answerText = _answerText,
                answerMessages = _answers,
                photo = _photo,
                queryLabelType = _queryLabelType
            )
        }
    }
}