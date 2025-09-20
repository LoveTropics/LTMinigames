package com.lovetropics.minigames.client.game.trivia;

import com.lovetropics.minigames.common.content.river_race.RiverRaceTexts;
import com.lovetropics.minigames.common.content.river_race.behaviour.TriviaBehaviour;
import com.lovetropics.minigames.common.content.river_race.block.TriviaBlockEntity;
import com.lovetropics.minigames.common.core.network.trivia.RequestTriviaStateUpdateMessage;
import com.lovetropics.minigames.common.core.network.trivia.SelectTriviaAnswerMessage;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class TriviaQuestionScreen extends Screen {
    public static class AutoUpdatingTextWidget extends AbstractStringWidget {
        private final Supplier<Component> messageSupplier;

        public AutoUpdatingTextWidget(Supplier<Component> message, Font font) {
            super(0, 0, 0, 0, Component.empty(), font);
            messageSupplier = message;
        }

        @Override
		public AutoUpdatingTextWidget setColor(int pColor) {
            super.setColor(pColor);
            return this;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int i, int i1, float v) {
            Component component = messageSupplier.get();
			MultiLineLabel label = MultiLineLabel.create(getFont(), component);
			label.renderCentered(graphics, getX(), getY(), 9, getColor());
        }
    }

    //TODO: make this a thing that actually looks like a good design
    public static class AnswerButton extends Button {
        protected AnswerButton(Builder builder) {
            super(builder);
        }

        public void setActive(boolean active){
            this.active = active;
            if(active){
                setAlpha(1);
            } else {
                setAlpha(0.5f);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private final TriviaBehaviour.TriviaQuestion question;
    private final BlockPos triviaBlockPos;
    private TriviaBlockEntity.TriviaBlockState triviaBlockState;
    private final Set<AnswerButton> buttons = new HashSet<>();

    public TriviaQuestionScreen(BlockPos triviaBlockPos, TriviaBehaviour.TriviaQuestion question, TriviaBlockEntity.TriviaBlockState blockState) {
        super(RiverRaceTexts.TRIVIA_SCREEN_TITLE);
        this.triviaBlockPos = triviaBlockPos;
        this.question = question;
        triviaBlockState = blockState;
    }

    @Override
    protected void init() {
        int maxWidth = Math.max(Window.BASE_WIDTH - 40, (width * 2) / 3);
        LinearLayout layout = LinearLayout.vertical().spacing(25);

        buttons.clear();
        layout.defaultCellSetting().alignHorizontallyCenter();
        layout.addChild(new AutoUpdatingTextWidget(() -> {
            if(triviaBlockState.lockedOut()) {
                long remainingSeconds = (triviaBlockState.unlocksAt() - Minecraft.getInstance().level.getGameTime()) / SharedConstants.TICKS_PER_SECOND;
                return RiverRaceTexts.TRIVIA_SCREEN_LOCKED_OUT.apply(Component.literal(remainingSeconds + "s").withStyle(ChatFormatting.GOLD));
            }
            return Component.empty();
        }, font));
        layout.addChild(new FocusableTextWidget(maxWidth, question.question(), font));

        boolean lockedOut = triviaBlockState.lockedOut();

        char ordinal = 'a';
        int ordinalWidth = 30;
		List<TriviaBehaviour.TriviaQuestion.TriviaQuestionAnswer> answers = question.answers();
		for (int i = 0; i < answers.size(); i++) {
			TriviaBehaviour.TriviaQuestion.TriviaQuestionAnswer answer = answers.get(i);
			LinearLayout answerLayout = LinearLayout.horizontal();
			answerLayout.defaultCellSetting().alignVerticallyMiddle();

			answerLayout.addChild(new StringWidget(Component.literal(ordinal + ") "), font));

            int answerIndex = i;
			AnswerButton button = new AnswerButton(Button.builder(answer.text(), b -> handleAnswerClick(answerIndex)).width(maxWidth - ordinalWidth));
			ordinal++;
			button.setActive(!lockedOut);
			buttons.add(button);
			answerLayout.addChild(button, answerLayout.newCellSettings().alignHorizontallyRight());

			layout.addChild(answerLayout);
		}

        layout.arrangeElements();
        FrameLayout.centerInRectangle(layout, 0, 0, width, height);
        layout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void tick() {
        super.tick();
        if(!Minecraft.getInstance().player.canInteractWithBlock(triviaBlockPos, 4.0)){
            onClose();
        }
        if(triviaBlockState.lockedOut() && triviaBlockState.unlocksAt() <= Minecraft.getInstance().level.getGameTime()){
            ClientPacketDistributor.sendToServer(new RequestTriviaStateUpdateMessage(triviaBlockPos));
        }
    }

    private void handleAnswerClick(int answerIndex) {
        ClientPacketDistributor.sendToServer(new SelectTriviaAnswerMessage(triviaBlockPos, answerIndex));
	}

    public void handleAnswerResponse(TriviaBlockEntity.TriviaBlockState triviaBlockState){
        this.triviaBlockState = triviaBlockState;
        for (AnswerButton button : buttons) {
            button.setActive(!triviaBlockState.lockedOut());
        }
        if (triviaBlockState.isAnswered()) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
