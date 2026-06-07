# Code of Conduct

## Our Pledge

Abysner is built with simplicity and correctness in mind, and we want the community around it to
reflect those same values. We pledge to make participation in this project a welcoming and
respectful experience for everyone, regardless of background, experience level, or diving
certification.

## Our Standards

Examples of behavior that contributes to a positive environment:

- Being respectful and constructive in discussions, issues, and pull requests
- Giving and receiving technical feedback graciously and without making it personal
- Being honest about the limits of your expertise, especially on safety-relevant topics
- Backing technical claims with references, test results, or validated sources where possible
- Properly crediting the sources of content you contribute
- Respecting the project's scope and direction, even when a contribution is not accepted
- Taking responsibility for your actions, and committing to repairing harm when it occurs

Examples of behavior that is not acceptable:

- Harassment, discrimination, or personal attacks of any kind
- Dismissing safety concerns without justification
- Submitting unverified changes to decompression logic or other safety-critical algorithms
- Misrepresenting test results or reference plan comparisons
- Spam, off-topic self-promotion, or low-effort contributions with no intent to improve

## Safety and Correctness

Divers use Abysner to plan real dives. That means correctness is not optional. Issues that affect
decompression calculations, gas planning, or ascent and descent modeling are treated with extra
care.

If you think you have found a bug that could lead to an unsafe dive plan, please **open an issue
right away** and describe it clearly. Do not wait until you have a fix ready. The community will
treat it as a priority.

Algorithmic contributions must be validated against the reference plans in the README, and ideally
reviewed by someone with relevant technical diving knowledge. Unverified or speculative changes to
core algorithms will not be merged, no matter how well-intentioned.

## AI Tools

As noted in the README, AI assistance has a place in this project: writing boilerplate, surfacing
bugs, exploring ideas. But it cannot replace correctness, and a safety-relevant application like a
dive planner must be deterministic.

Contributors are welcome to use AI tools, but:

- Do not submit AI-generated decompression logic or algorithm changes without manually verifying them
- Do not present AI-generated safety-critical content as your own verified work
- Every line of code that affects a dive calculation must be understood and checked by a person

## Enforcement

Project maintainers are responsible for clarifying and enforcing this Code of Conduct. You can
report unacceptable behavior by opening a private discussion or contacting the maintainers directly
through GitHub.

When a violation occurs, maintainers will do their best to respond promptly and handle it fairly.
Depending on the severity and whether it is a first or repeated incident, responses may include:

1. **Warning.** A private written note explaining the issue and what is expected going forward.
2. **Temporary cooldown.** A time-limited restriction on participation, giving everyone involved
   time to process the situation.
3. **Temporary suspension.** A pause from the project with conditions for return.
4. **Permanent ban.** Reserved for serious or repeated violations where other steps have not worked.

Maintainers may also remove, edit, or reject comments, commits, issues, and other contributions
that violate this Code of Conduct.

## Scope

This Code of Conduct applies in all project spaces, including GitHub issues, pull requests,
discussions, and any other official project channels.

## Attribution

This Code of Conduct is adapted from the [Contributor Covenant, version 3.0](https://www.contributor-covenant.org/version/3/0/code_of_conduct/),
with additions specific to the safety-critical nature of Abysner.
