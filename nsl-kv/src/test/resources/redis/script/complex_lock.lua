-- KEYS[1] - lock key (e.g., ingestor:shard_lock:0)
-- ARGV[1] - ID of our Pod (e.g., pod-123)
-- ARGV[2] - TTL (lease time) in seconds

local lockKey = KEYS[1]
local podId = ARGV[1]
local ttl = tonumber(ARGV[2])

local currentOwner = redis.call('GET', lockKey)

if not currentOwner or currentOwner == podId then
  -- atomically
  redis.call('SET', lockKey, podId, 'EX', ttl)
  return 1 -- success
else
  return 0 -- denied, shard has a different owner
end
