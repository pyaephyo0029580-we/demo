using UnityEngine;
using UnityEngine.InputSystem;

public class Playermove : MonoBehaviour
{
    [SerializeField] private float moveSpeed = 10f;

    private Rigidbody2D rb;
    private Animator animator;
    private Vector2 moveInput;

    private void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        animator = GetComponent<Animator>();
    }

    private void FixedUpdate()
    {
        rb.linearVelocity = moveInput * moveSpeed;
    }

    // Player Input -> Send Messages က ဒီ function ကိုခေါ်မယ်
    public void OnMove(InputValue value)
    {
        moveInput = value.Get<Vector2>();

        if (animator != null)
        {
            bool isWalking = moveInput.sqrMagnitude > 0.01f;

            animator.SetBool("Iswalking", isWalking);
            animator.SetFloat("InputX", moveInput.x);
            animator.SetFloat("InputY", moveInput.y);

            if (isWalking)
            {
                animator.SetFloat("LastInputX", moveInput.x);
                animator.SetFloat("LastInputY", moveInput.y);
            }
        }
    }
}