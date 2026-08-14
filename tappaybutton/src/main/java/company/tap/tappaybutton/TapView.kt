package company.tap.tappaybutton


interface TapView<T: BaseTextTheme> {
    fun setTheme(theme: T)
}