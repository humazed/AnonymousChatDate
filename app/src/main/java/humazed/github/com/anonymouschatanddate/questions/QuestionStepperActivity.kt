package humazed.github.com.anonymouschatanddate.questions

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import humazed.github.com.anonymouschatanddate.R
import humazed.github.com.anonymouschatanddate.chat.MainChatActivity
import humazed.github.com.kotlinandroidutils.ContextLocale
import kotlinx.android.synthetic.main.activity_questions_stepper.*
import kotlinx.android.synthetic.main.question_item.view.*
import moe.feng.common.stepperview.IStepperAdapter
import moe.feng.common.stepperview.VerticalStepperItemView
import org.jetbrains.anko.startActivity

class QuestionStepperActivity : AppCompatActivity(), IStepperAdapter {

    private var questions = arrayOf(
            "تجد صعوبة في تقديم نفسك للآخرين",
            "لا ترغب عادة في بدء المحادثات",
            "نادرًا ما يستطيع الأفراد مضايقتك",
            "القدرة على وضع خطة والالتزام بها أهم جزء في المشروع اليومي",
            "لا تسمح للآخرين بالتأثير على أفعالك",
            "مشاعرك تتحكم فيك أكثر من تحكمك فيها",
            "أنت شخص متحفظ وهادئ إلى حد ما",
            "تقلق كثيرًا بخصوص ما يعتقده الآخرون",
            "تشعر أنك قلق جدًا في المواقف العصيبة",
            "ترى أن محبة الآخرين لك أهم بكثير من التحلي بالقوة",
            "كثيرًا ما تأخذ زمام المبادرة في المواقف الاجتماعية"
    )

    private val choices = mutableListOf<String>()

    public override fun onCreate(savedInstanceStat: Bundle?) {
        super.onCreate(savedInstanceStat)
        setContentView(R.layout.activity_questions_stepper)
        verticalStepperView!!.stepperAdapter = this
    }

    override fun getTitle(index: Int): CharSequence {
        return "question ${index + 1}"
    }

    override fun getSummary(index: Int): CharSequence? {
        return when (index) {
            0 -> Html.fromHtml("Summarized if needed" + if (verticalStepperView.currentStep > index) "; <b>isDone!</b>" else "")
            questions.size -> Html.fromHtml("Last step" + if (verticalStepperView.currentStep > index) "; <b>isDone!</b>" else "")
            else -> null
        }
    }

    override fun size(): Int = questions.size

    override fun onCreateCustomView(index: Int, context: Context, parent: VerticalStepperItemView): View {
        return LayoutInflater.from(context).inflate(R.layout.question_item, parent, false).apply {

            questionTextView.text = questions[index]

            yesButton.setOnClickListener { saveChoice(context.getString(R.string.yes)) }
            noButton.setOnClickListener { saveChoice(context.getString(R.string.no)) }
        }
    }

    private fun saveChoice(choice: String) {
        choices.add(choice)
        if (!verticalStepperView.nextStep()) startActivity<MainChatActivity>()
    }

    override fun onShow(index: Int) {}
    override fun onHide(index: Int) {}

    override fun attachBaseContext(newBase: Context?) = super.attachBaseContext(ContextLocale.wrap(newBase!!))
}
