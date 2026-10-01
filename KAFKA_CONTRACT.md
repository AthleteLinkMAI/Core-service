# Core ↔ Game contract

Core owns requests, accepted participants, and the sport catalog. Game owns round results and player ratings. Core does not store a second copy of confirmed round results.

All Core-to-Game commands use the `game.commands` topic and the **request UUID as the Kafka key**. Game must process commands for a request in partition order. The Core outbox publishes them in creation order and retries failed sends, so delivery is **at least once**. Consumers must deduplicate. JSON is sent as UTF-8 without Java class/type headers.

Round submission (`POST /requests/{requestId}/rounds/{roundNumber}/result` returns HTTP 202):

```json
{
  "type": "ROUND_RESULT_SUBMITTED",
  "eventId": "11111111-1111-1111-1111-111111111111",
  "requestId": "22222222-2222-2222-2222-222222222222",
  "roundNumber": 1,
  "sportId": 1,
  "recordedBy": "33333333-3333-3333-3333-333333333333",
  "players": [
    { "userId": "44444444-4444-4444-4444-444444444444", "outcome": "WIN" },
    { "userId": "55555555-5555-5555-5555-555555555555", "outcome": "LOSS" }
  ]
}
```

`eventId` identifies a delivery; `(requestId, roundNumber)` identifies the game. Game must keep at most one result for that pair and at most one entry for each player in that result. It must not credit ratings twice when a message is retried or the HTTP request is repeated. Core validates organizer, round range, equal teams, disjoint player IDs, and accepted membership before queuing the command. Game still owns the final acceptance and storage decision. If rating depends on round order, Game must reject or postpone a later round until earlier rounds are confirmed.

Completion request (`POST /requests/{requestId}/complete` returns HTTP 202):

```json
{
  "type": "CLOSE_REQUESTED",
  "requestId": "22222222-2222-2222-2222-222222222222",
  "attemptId": "66666666-6666-6666-6666-666666666666",
  "expectedRounds": 3
}
```

Game verifies confirmed results for rounds `1..expectedRounds`, then publishes exactly one logical answer to `game.replies`, again keyed by `requestId`:

```json
{
  "type": "CLOSE_REQUEST_RESOLVED",
  "requestId": "22222222-2222-2222-2222-222222222222",
  "attemptId": "66666666-6666-6666-6666-666666666666",
  "successful": true,
  "reason": null
}
```

For refusal, `successful` is `false` and `reason` explains the missing or rejected round. Core applies a reply only while the request is `CONFIRMATION` and `attemptId` matches. Success produces `COMPLETED`; refusal returns to `ACTIVE` and exposes `completionErrorMessage` in request details. Duplicate or stale replies have no effect. The client polls `GET /requests/{requestId}` for the final status. Result reads must go to Game; Core no longer serves `GET /requests/{requestId}/rounds`.

This contract requires the Game consumer, result tables, unique constraints, scoring, and reply producer to be implemented there. Core alone cannot complete an end-to-end run.
