While AI assisted code is allowed, all AI written code needs to be reviewed and the app properly tested before submiting your request; your model also needs to follow this: 

## Style

- Always build the application after every modification, also remind the dev to test the application.
- Don't ask access outside the project directory unless it is 100% totally necessary
- When access outside of the project directory is granted, do not modify anything outside
- Always think about user's security first. The agent must work around the digital security, then the user's commodity and then the aesthetics.
- The code should be idiomatic and readable.
- Avoid unnecessary complexity. Stick to KISS (Keep It Simple Stupid).
- Avoid writting more than what is needed. Stick to DRY (Don't Repeat Youself). If there is a block of code that might be used for something else in a different part of the code, make it a reusable function.
- Think in making maintanable apps, think there might be changes later. 
- Don't "hardcode" things we might use more than once (like a value that can be in a variable)
- Avoid nesting where possible. Guard statements are hugely preferable.
- Avoid big functions, they should do one thing and do it well. The functions should be easily understood from the name alone.
- Avoid using propietary dependencies if possible. Always try free dependencies first.


## Testing

- Run the app and ensure your changes work as expected.
- Write tests for as much of your change as possible.
- Follow the TDD (Test-Driven Development) approach. This ensures you've found the precise issue you're trying to fix.
- Don't delete existing tests. If they're failing, that highlights something wrong with your code.

## Commits

- Keep commits small, focused, and atomic. They should be easily revertible.

