# Page snapshot

```yaml
- generic [ref=e3]:
  - navigation:
    - generic:
      - link "SignalOS":
        - /url: /
        - generic: SignalOS
  - main [ref=e4]:
    - generic [ref=e8]:
      - generic [ref=e10]: Activate Identity
      - generic [ref=e12]: Enter the 6-digit code sent to your email
      - generic [ref=e14]:
        - textbox "000000" [ref=e16]
        - button "Verify and Activate" [ref=e17] [cursor=pointer]:
          - generic [ref=e18]: Verify and Activate
        - button "Resend Verification Code" [ref=e20] [cursor=pointer]:
          - generic [ref=e21]: Resend Verification Code
        - paragraph [ref=e22]: Identity verification is a mandatory protocol for Signal OS access.
  - contentinfo:
    - generic:
      - generic: 
      - generic: Global Governance Standard
    - generic: © 2026 Open Civic Signal OS. Protocol v0.3.0-hardened
    - generic:
      - generic: 
      - generic: 
```