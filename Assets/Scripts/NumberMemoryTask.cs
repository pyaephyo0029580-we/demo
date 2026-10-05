using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

public class NumberMemoryTask : MonoBehaviour
{
    [Header("UI")]
    public GameObject taskPanel;
    public TMP_Text numberDisplay;
    public TMP_Text playerInput;

    [Header("Number Buttons")]
    public Button[] numberButtons;

    [Header("Settings")]
    public int numberLength = 6;
    public float showTime = 3f;

    private string correctNumber = "";
    private string currentInput = "";

    private void Start()
    {
        // Task panel ကို အစမှာ ဖျောက်ထားမယ်
        taskPanel.SetActive(false);
    }

    // Task စတဲ့အချိန်မှာ ခေါ်မယ့် Function 
    public void StartTask()
    {
        taskPanel.SetActive(true);

        currentInput = "";
        playerInput.text = "";

        GenerateNumber();

        StartCoroutine(ShowNumber());
    }

    // Random 6-digit number ထုတ်မယ်
    // နံပါတ်တစ်လုံးစီ မထပ်အောင်လုပ်ထားတယ်
    private void GenerateNumber()
    {
        correctNumber = "";

        List<int> availableNumbers = new List<int>();

        // 0 - 9 ထည့်မယ်
        for (int i = 0; i <= 9; i++)
        {
            availableNumbers.Add(i);
        }

        // numberLength အတိုင်း random ရွေးမယ်
        for (int i = 0; i < numberLength; i++)
        {
            int randomIndex = Random.Range(0, availableNumbers.Count);

            correctNumber += availableNumbers[randomIndex].ToString();

            // ရွေးပြီးသား number ကို ပြန်မရွေးနိုင်အောင် ဖယ်မယ်
            availableNumbers.RemoveAt(randomIndex);
        }

        Debug.Log("Correct Number: " + correctNumber);
    }

    // Number ကို ခဏပြမယ်
    private IEnumerator ShowNumber()
    {
        // Number ပြနေချိန်မှာ button တွေ ပိတ်ထားမယ်
        SetButtonsInteractable(false);

        numberDisplay.text = correctNumber;

        // 3 seconds စောင့်မယ်
        yield return new WaitForSeconds(showTime);

        // Number ဖျောက်မယ်
        numberDisplay.text = "Remember!";

        // Button တွေ ပြန်ဖွင့်မယ်
        SetButtonsInteractable(true);
    }

    // Number button နှိပ်တဲ့အခါ
    public void NumberClicked(int number)
    {
        // 6 လုံးပြည့်သွားရင် ထပ်မထည့်နိုင်
        if (currentInput.Length >= numberLength)
            return;

        currentInput += number.ToString();

        playerInput.text = currentInput;

        // 6 လုံးပြည့်ရင် answer စစ်မယ်
        if (currentInput.Length == numberLength)
        {
            CheckAnswer();
        }
    }

    // Answer စစ်မယ်
    private void CheckAnswer()
    {
        if (currentInput == correctNumber)
        {
            TaskComplete();
        }
        else
        {
            TaskFailed();
        }
    }

    // မှန်ရင်
    private void TaskComplete()
    {
        numberDisplay.text = "✓ TASK COMPLETE";

        SetButtonsInteractable(false);

        Debug.Log("NUMBER MEMORY TASK COMPLETE!");
    }

    // မှားရင်
    private void TaskFailed()
    {
        numberDisplay.text = "✗ WRONG!";

        SetButtonsInteractable(false);

        StartCoroutine(RetryTask());
    }

    // မှားရင် number အသစ်နဲ့ ပြန်စမယ်
    private IEnumerator RetryTask()
    {
        yield return new WaitForSeconds(1.5f);

        currentInput = "";
        playerInput.text = "";

        // Number အသစ်ထုတ်မယ်
        GenerateNumber();

        // Number အသစ်ကို ပြန်ပြမယ်
        StartCoroutine(ShowNumber());
    }

    // Button တွေ Enable / Disable
    private void SetButtonsInteractable(bool value)
    {
        foreach (Button button in numberButtons)
        {
            button.interactable = value;
        }
    }
}