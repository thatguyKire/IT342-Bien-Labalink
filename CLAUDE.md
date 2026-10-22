# Git commit and PR rules

## Attribution
- Do NOT add any `Co-Authored-By:` trailer to commit messages. This includes `Co-Authored-By: Claude <noreply@anthropic.com>`.
- Do NOT add "🤖 Generated with Claude Code" or any similar line to commit messages, PR titles, PR descriptions, or code comments.
- Do NOT mention Claude, Claude Code, AI, or Anthropic in commits or PRs unless I explicitly ask.
- The only author of every commit is the configured git user (`git config user.name` / `user.email`). Do not change these values.

## Commit messages
- Use a short imperative summary line (max ~72 characters), e.g. `Add login validation`.
- Add a body only when the change needs explaining.
- End the message after the last line of the body. Do not add trailers of any kind.

## Before committing
- Check the final commit message and remove any attribution lines before running `git commit`.
- Never run `git push --force` or rewrite history unless I ask for it.
