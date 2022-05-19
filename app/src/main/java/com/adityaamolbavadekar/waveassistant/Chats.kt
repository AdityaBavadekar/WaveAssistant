package com.adityaamolbavadekar.waveassistant

data class Chats(
    var label: String,
    var questions: List<String>,
    var answers: List<String>
) {

    class Builder() {

        private var _label = ""
        private var _questions = listOf<String>()
        private var _answers = listOf<String>()

        fun withLabel(text: String): Builder {
            _label = text
            return this
        }

        fun withQuestions(texts: List<String>): Builder {
            _questions = texts
            return this
        }

        fun withAnswers(texts: List<String>): Builder {
            _answers = texts
            return this
        }

        fun build(): Chats {
            return Chats(label = _label, questions = _questions, answers = _answers)
        }

    }

}