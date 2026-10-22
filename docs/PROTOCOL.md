# NecromancerFX Bridge Protocol v1

Proposed plugin-message channel: `necromancer:bridge`. Packets use `[u8 type][payload]`; multi-byte numeric values are big-endian; strings are UTF-8 with a Minecraft-style VarInt byte length.

| Type | Name | Direction | Payload |
|---|---|---|---|
| 0x01 | HELLO | Client → Server | VarInt version, string(32) mod version, string(64) nonce |
| 0x02 | CHALLENGE | Server → Client | string(64) nonce |
| 0x03 | ACK | Both | client sends string(64) nonce; server sends boolean accepted |
| 0x04 | MOB_SYNC | Server → Client | UUID, string(64) mob ID, string(128) fallback entity, float scale |
| 0x05 | SUMMON | Server → Client | UUID, string(64) mob ID |
| 0x06 | ANIMATION | Server → Client | UUID, string(64) animation ID |
| 0x07 | DESPAWN | Server → Client | UUID |
| 0x08 | ERROR | Server → Client | string(64) code, string(256) message |

This protocol is documentation only in this initial plugin implementation. A compatible client mod is not included, and handshake/cosmetic packets are not yet implemented. Handshake would indicate compatibility, not authenticate a client.
