package com.jetdrone.vertx.mods.redis;

import com.jetdrone.vertx.mods.redis.command.*;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import com.jetdrone.vertx.mods.redis.reply.*;

import java.nio.charset.Charset;

@SuppressWarnings("unused")
public class RedisClientBusMod extends BusModBase implements Handler<Message<JsonObject>> {

    private RedisClientBase redisClient;
    private Charset charset;
    private boolean binary;

    private static final KeyValue KV = new KeyValue("key", "value", "keyvalues");
    private static final KeyValue FV = new KeyValue("field", "value", "fieldvalues");
    private static final KeyValue SM = new KeyValue("score", "member", "scoremembers");

    private static final Option WITHSCORES = new Option("withscores");
    private static final Option ALPHA = new Option("alpha");

    private static final OrOption ASC_OR_DESC = new OrOption(new Option("asc"), new Option("desc"));
    private static final OrOption BEFORE_OR_AFTER = new OrOption(new Option("before"), new Option("after"));
    private static final OrOption SAVE_OR_NOSAVE = new OrOption(new Option("save"), new Option("nosave"));

    private static final NamedValue BY = new NamedValue("by");
    private static final NamedValue WEIGTHS = new NamedValue("weights");
    private static final NamedValue STORE = new NamedValue("store");
    private static final NamedValue GET = new NamedValue("get");
    private static final NamedValue AGGREGATE = new NamedValue("aggregate");

    private static final NamedKeyValue LIMIT = new NamedKeyValue("limit", "offset", "count");

    @Override
    public void start() {
        super.start();

        String host = getOptionalStringConfig("host", "localhost");
        int port = getOptionalIntConfig("port", 6379);
        String encoding = getOptionalStringConfig("encoding", null);
        binary = getOptionalBooleanConfig("binary", false);

        if (binary) {
            logger.warn("Binary mode is not implemented yet!!!");
        }

        if (encoding != null) {
            charset = Charset.forName(encoding);
        } else {
            charset = Charset.defaultCharset();
        }

        redisClient = new RedisClientBase(vertx, logger, host, port);
        redisClient.connect(null);
        
        String address = getOptionalStringConfig("address", "vertx.mod-redis-io");
        eb.registerHandler(address, this);
    }
    
    @Override
    public void handle(final Message<JsonObject> message) {

        String redisCommand = message.body.getString("command");

        if (redisCommand == null) {
            sendError(message, "command must be specified");
            return;
        }

        final JSONCommand command = new JSONCommand(redisCommand, message, charset);
        boolean array2object = false;

        try {
            switch (redisCommand) {
                // no argument
                case "randomkey":
                case "discard":
                case "exec":
                case "multi":
                case "unwatch":
                case "script flush":
                case "script kill":
                case "ping":
                case "quit":
                case "bgrewriteaof":
                case "bgsave":
                case "client list":
                case "client getname":
                case "config resetstat":
                case "dbsize":
                case "debug segfault":
                case "flushall":
                case "flushdb":
                case "lastsave":
                case "monitor":
                case "save":
                case "sync":
                case "time":
                    break;
                // argument "key"
                case "dump":
                case "exists":
                case "persist":
                case "pttl":
                case "ttl":
                case "type":
                case "decr":
                case "get":
                case "incr":
                case "strlen":
                case "hkeys":
                case "hlen":
                case "hvals":
                case "llen":
                case "lpop":
                case "rpop":
                case "scard":
                case "smembers":
                case "spop":
                case "zcard":
                case "debug object":
                    // arguments "key" ["key"...]
                case "del":
                case "mget":
                case "sdiff":
                case "sinter":
                case "sunion":
                case "watch":
                    command.arg("key");
                    break;
                case "hgetall":
                    array2object = true;
                    command.arg("key");
                    break;
                // argument "pattern"
                case "keys":
                // argument "pattern" ["pattern"...]
                case "psubscribe":
                    command.arg("pattern");
                    break;
                // argument "password"
                case "auth":
                    command.arg("password");
                    break;
                // argument "message"
                case "echo":
                    command.arg("message");
                    break;
                // argument "index"
                case "select":
                    command.arg("index");
                    break;
                // argument "connection-name"
                case "client setname":
                    command.arg("connection-name");
                    break;
                // argument "parameter"
                case "config get":
                    command.arg("parameter");
                    break;
                // argument "script"
                case "script load":
                    // argument "script" ["script"...]
                case "script exists":
                    command.arg("script");
                    break;
                // argument "channel" ["channel"...]
                case "subscribe":
                    command.arg("channel");
                    break;
                // arguments "key" "value"
                case "append":
                case "getset":
                case "set":
                case "setnx":
                case "lpushx":
                case "rpushx":
                    // arguments "key" "value" ["value"...]
                case "lpush":
                case "rpush":
                    command.arg("key");
                    command.arg("value");
                    break;
                // argumens "key" "seconds"
                case "expire":
                    command.arg("key");
                    command.arg("seconds");
                    break;
                // argumens "key" "timestamp"
                case "expireat":
                    command.arg("key");
                    command.arg("timestamp");
                    break;
                // argumens "key" "db"
                case "move":
                    command.arg("key");
                    command.arg("db");
                    break;
                // argumens "key" "milliseconds"
                case "pexpire":
                    command.arg("key");
                    command.arg("milliseconds");
                    break;
                // argumens "key" "milliseconds-timestamp"
                case "pexpireat":
                    command.arg("key");
                    command.arg("milliseconds-timestamp");
                    break;
                // argumens "key" "newkey"
                case "rename":
                case "renamenx":
                    command.arg("key");
                    command.arg("newkey");
                    break;
                // arguments "key" "decrement"
                case "decrby":
                    command.arg("key");
                    command.arg("decrement");
                    break;
                // arguments "key" "offset"
                case "getbit":
                    command.arg("key");
                    command.arg("offset");
                    break;
                // arguments "key" "increment"
                case "incrby":
                case "incrbyfloat":
                    command.arg("key");
                    command.arg("increment");
                    break;
                // arguments "key" "field"
                case "hexists":
                case "hget":
                    // arguments "key" "field" ["field"...]
                case "hdel":
                case "hmget":
                    command.arg("key");
                    command.arg("field");
                    break;
                // arguments "key" "index"
                case "lindex":
                    command.arg("key");
                    command.arg("index");
                    break;
                // arguments "key" "member"
                case "sismember":
                case "zrank":
                case "zrevrank":
                case "zscore":
                // arguments "key" "member" ["member"...]
                case "sadd":
                case "srem":
                case "zrem":
                    command.arg("key");
                    command.arg("member");
                    break;
                // arguments "source" "destination"
                case "rpoplpush":
                    command.arg("source");
                    command.arg("destination");
                    break;
                // arguments "channel" "message"
                case "publish":
                    command.arg("channel");
                    command.arg("message");
                    break;
                // arguments "host" "port"
                case "slaveof":
                    command.arg("host");
                    command.arg("port");
                    break;
                // arguments "ip" "port"
                case "client kill":
                    command.arg("ip");
                    command.arg("port");
                    break;
                // arguments "parameter" "value"
                case "config set":
                    command.arg("parameter");
                    command.arg("value");
                    break;
                // arguments "destination" "key" ["key"...]
                case "sdiffstore":
                case "sinterstore":
                case "sunionstore":
                    command.arg("destination");
                    command.arg("key");
                    break;
                // arguments "key" ["key"...] timeout
                case "blpop":
                case "brpop":
                    command.arg("key");
                    command.arg("timeout");
                    break;
                // arguments "key" "ttl" "serialized-value"
                case "restore":
                    command.arg("key");
                    command.arg("ttl");
                    command.arg("serialized-value");
                    break;
                // arguments "key" "start" "end"
                case "getrange":
                    command.arg("key");
                    command.arg("start");
                    command.arg("end");
                    break;
                // arguments "key" "milliseconds" "value"
                case "psetex":
                    command.arg("key");
                    command.arg("milliseconds");
                    command.arg("value");
                    break;
                // arguments "key" "offset" "value"
                case "setbit":
                case "setrange":
                    command.arg("key");
                    command.arg("offset");
                    command.arg("value");
                    break;
                // arguments "key" "seconds" "value"
                case "setex":
                    command.arg("key");
                    command.arg("seconds");
                    command.arg("value");
                    break;
                // arguments "key" "field" "increment"
                case "hincrby":
                case "hincrbyfloat":
                    command.arg("key");
                    command.arg("field");
                    command.arg("increment");
                    break;
                // arguments "key" "field" "value"
                case "hset":
                case "hsetnx":
                    command.arg("key");
                    command.arg("field");
                    command.arg("value");
                    break;
                // arguments "source" "destination" "timeout"
                case "brpoplpush":
                    command.arg("source");
                    command.arg("destination");
                    command.arg("timeout");
                    break;
                // arguments "key" "start" "stop"
                case "lrange":
                case "ltrim":
                case "zremrangebyrank":
                    command.arg("key");
                    command.arg("start");
                    command.arg("stop");
                    break;
                // arguments "key" "count" "value"
                case "lrem":
                    command.arg("key");
                    command.arg("count");
                    command.arg("value");
                    break;
                // arguments "key" "index" "value"
                case "lset":
                    command.arg("key");
                    command.arg("index");
                    command.arg("value");
                    break;
                // arguments "source" "destination" "member"
                case "smove":
                    command.arg("source");
                    command.arg("destination");
                    command.arg("member");
                    break;
                // arguments "key" "min" "max"
                case "zcount":
                case "zremrangebyscore":
                    command.arg("key");
                    command.arg("min");
                    command.arg("max");
                    break;
                // arguments "key" "increment" "member"
                case "zincrby":
                    command.arg("key");
                    command.arg("increment");
                    command.arg("member");
                    break;
                // arguments "operation" "destkey" "key" ["key"...]
                case "bitop":
                    command.arg("operation");
                    command.arg("destkey");
                    command.arg("key");
                    break;
                // arguments "host" "port" "key" "destination-db" "timeout"
                case "migrate":
                    command.arg("host");
                    command.arg("port");
                    command.arg("key");
                    command.arg("destination-db");
                    command.arg("timeout");
                    break;
                // argument ["section"]
                case "info":
                    command.optArg("section");
                    break;
                // argument ["pattern" ["pattern"...]]
                case "punsubscribe":
                    command.optArg("pattern");
                    break;
                // argument ["channel" ["channel"...]]
                case "unsubscribe":
                    command.optArg("channel");
                    break;
                // arguments "subcommand" ["argument"]
                case "slowlog":
                    command.arg("subcommand");
                    command.optArg("argument");
                    break;
                // arguments "subcommand" ["arguments"]
                case "object":
                    command.arg("subcommand");
                    command.optArg("arguments");
                    break;
                // arguments "key" ["count"]
                case "srandmember":
                    command.arg("key");
                    command.optArg("count");
                    break;
                // arguments "key" "start" "stop" ["withscores"]
                case "zrange":
                case "zrevrange":
                    command.arg("key");
                    command.arg("start");
                    command.arg("stop");
                    command.optArg(WITHSCORES);
                    break;
                // arguments KV: "key" "value" ["key" "value"...]
                case "mset":
                case "msetnx":
                    command.arg(KV);
                    break;
                // arguments "key" FV: "field" "value" ["field" "value"...]
                case "hmset":
                    command.arg("key");
                    command.arg(FV);
                    break;
                // arguments "key" SM: "score" "member" ["score" "member"...]
                case "zadd":
                    command.arg("key");
                    command.arg(SM);
                    break;
                // arguments "key" ["start"] ["end"]
                case "bitcount":
                    command.arg("key");
                    command.optArg("start");
                    command.optArg("end");
                    break;
                // arguments ["nosave"] ["save"]
                case "shutdown":
                    command.optArg(SAVE_OR_NOSAVE);
                    break;
                // arguments: key BEFORE|AFTER pivot value
                case "linsert":
                    command.arg("key");
                    command.arg(BEFORE_OR_AFTER);
                    command.arg("pivot");
                    command.arg("value");
                    break;
                // complex non generic: key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination]
                case "sort":
                    command.arg("key");
                    command.optArg(BY);
                    command.optArg(LIMIT);
                    command.optArg(GET);
                    command.optArg(ASC_OR_DESC);
                    command.optArg(ALPHA);
                    command.optArg(STORE);
                    break;
                // complex non generic: destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
                case "zinterstore":
                case "zunionstore":
                    command.arg("destination");
                    command.arg("numkeys");
                    command.arg("key");
                    command.optArg(WEIGTHS);
                    command.optArg(AGGREGATE);
                    break;
                // key min max [WITHSCORES] [LIMIT offset count]
                case "zrangebyscore":
                    command.arg("key");
                    command.arg("min");
                    command.arg("max");
                    command.optArg(WITHSCORES);
                    command.optArg(LIMIT);
                    break;
                case "zrevrangebyscore":
                    command.arg("key");
                    command.arg("max");
                    command.arg("min");
                    command.optArg(WITHSCORES);
                    command.optArg(LIMIT);
                    break;
                // script numkeys key [key ...] arg [arg ...]
                case "eval":
                    command.arg("script");
                    command.arg("numkeys");
                    command.arg("key");
                    command.arg("arg");
                    break;
                // sha1 numkeys key [key ...] arg [arg ...]
                case "evalsha":
                    command.arg("sha1");
                    command.arg("numkeys");
                    command.arg("key");
                    command.arg("arg");
                    break;
                default:
                    sendError(message, "Invalid command: " + command);
                    return;
            }

            // run redis command on server
            final boolean finalArray2object = array2object;
            redisClient.send(command.args, new Handler<Reply>() {
                @Override
                public void handle(Reply reply) {
                    processReply(message, reply, finalArray2object);
                }
            });
        } catch (RedisCommandError rce) {
            sendError(message, rce.getMessage());
        }
    }

    private void processReply(Message<JsonObject> message, Reply reply, boolean array2Object) {
        JsonObject replyMessage;

        switch (reply.getType()) {
            case Error:
                sendError(message, ((ErrorReply) reply).data());
                return;
            case Status:
                replyMessage = new JsonObject();
                replyMessage.putString("value", ((StatusReply) reply).data());
                sendOK(message, replyMessage);
                return;
            case Bulk:
                replyMessage = new JsonObject();
                replyMessage.putString("value", ((BulkReply) reply).asString(charset));
                sendOK(message, replyMessage);
                return;
            case MultiBulk:
                replyMessage = new JsonObject();
                MultiBulkReply mbreply = (MultiBulkReply) reply;
                if (array2Object) {
                    JsonObject bulk = new JsonObject();
                    Reply[] mbreplyData = mbreply.data();

                    for (int i = 0; i < mbreplyData.length; i+=2) {
                        BulkReply brKey = (BulkReply) mbreplyData[i];
                        BulkReply brValue = (BulkReply) mbreplyData[i+1];
                        bulk.putString(brKey.asString(charset), brValue.asString(charset));
                    }
                    replyMessage.putObject("value", bulk);
                } else {
                    JsonArray bulk = new JsonArray();
                    for (Reply r : mbreply.data()) {
                        bulk.addString(((BulkReply) r).asString(charset));
                    }
                    replyMessage.putArray("value", bulk);
                }
                sendOK(message, replyMessage);
                return;
            case Integer:
                replyMessage = new JsonObject();
                replyMessage.putNumber("value", ((IntegerReply) reply).data());
                sendOK(message, replyMessage);
                return;
            default:
                sendError(message, "Unknown message type");
        }
    }
}
