package com.github.masahitojp.botan;

import com.github.masahitojp.botan.brain.BotanBrain;
import com.github.masahitojp.botan.responder.BotanMessageResponder;
import com.github.masahitojp.botan.responder.BotanMessageResponderBuilder;
import com.github.masahitojp.botan.responder.BotanMessageResponderRegister;
import com.github.masahitojp.botan.responder.BotanMessageResponderSetter;
import com.github.masahitojp.botan.message.BotanMessage;
import com.github.masahitojp.botan.message.BotanMessageSimple;
import lombok.Getter;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;

public class Robot {
    private final Botan botan;
    @Getter
    private final List<BotanMessageResponder> listeners = new ArrayList<>();
    private final List<BotanMessageResponderSetter> actions = new ArrayList<>();
    private final List<BotanMessageResponderRegister> registers = new ArrayList<>();
    public Robot(final Botan botan) {
        this.botan = botan;
    }

    private void setActions() {
        final Reflections reflections = new Reflections();
        Set<Class<? extends BotanMessageResponderRegister>> classes = reflections.getSubTypesOf(BotanMessageResponderRegister.class);
        classes.forEach(clazz -> {
            try {
                final BotanMessageResponderRegister register = clazz.newInstance();
                register.initialize(this);
                register.register(this);
                registers.add(register);
            } catch (final InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }


    final void run() {
        setActions();
        this.actions.forEach(x -> listeners.add(BotanMessageResponderBuilder.build(this.botan, x)));
    }

    public final void receive(BotanMessageSimple message) {
        this.listeners.stream().filter(listener -> message.getBody() != null).forEach(listener -> {
            // 自分の発言ははじく
            if (!message.getFromName().equals(botan.getName())) {
                final Matcher matcher = listener.getPattern().matcher(message.getBody());
                if (matcher.find()) {
                    listener.apply(
                            new BotanMessage(
                                    this.botan,
                                    matcher,
                                    message
                            ));
                }
            }
        });
    }


    final void beforeShutdown() {
        this.registers.forEach(BotanMessageResponderRegister::beforeShutdown);
    }

    public final void hear(final String pattern, final String description, final Consumer<BotanMessage> action) {
        actions.add(new BotanMessageResponderSetter() {
            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public String getPatternString() {
                return pattern;
            }

            @Override
            public void accept(BotanMessageResponder botanMessageResponder) {
                botanMessageResponder.setAllReceived(true);
                botanMessageResponder.setDescription(description);
                botanMessageResponder.setPattern(pattern);
                botanMessageResponder.setAction(action);
            }
        });
    }

    public final void respond(final String pattern, final String description, final Consumer<BotanMessage> action) {
        actions.add(new BotanMessageResponderSetter() {
            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public String getPatternString() {
                return pattern;
            }

            @Override
            public void accept(BotanMessageResponder botanMessageResponder) {
                botanMessageResponder.setAllReceived(false);
                botanMessageResponder.setDescription(description);
                botanMessageResponder.setPattern(pattern);
                botanMessageResponder.setAction(action);
            }
        });
    }


    @SuppressWarnings("unused")
    public final String getName() {
        return this.botan.getName();
    }

    @SuppressWarnings("unused")
    public final void send(final BotanMessageSimple message) {
        this.botan.say(new BotanMessage(this.botan, null, message));
    }

    @SuppressWarnings("unused")
    public final BotanBrain getBrain() {
        return this.botan.brain;
    }
}
