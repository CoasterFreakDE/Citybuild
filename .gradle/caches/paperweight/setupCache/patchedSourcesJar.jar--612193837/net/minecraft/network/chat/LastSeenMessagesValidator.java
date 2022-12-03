package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

public class LastSeenMessagesValidator {
    private static final int NOT_FOUND = Integer.MIN_VALUE;
    private LastSeenMessages lastSeenMessages = LastSeenMessages.EMPTY;
    private final ObjectList<LastSeenMessages.Entry> pendingEntries = new ObjectArrayList<>();

    public void addPending(LastSeenMessages.Entry entry) {
        this.pendingEntries.add(entry);
    }

    public int pendingMessagesCount() {
        return this.pendingEntries.size();
    }

    private boolean hasDuplicateProfiles(LastSeenMessages messages) {
        Set<UUID> set = new HashSet<>(messages.entries().size());

        for(LastSeenMessages.Entry entry : messages.entries()) {
            if (!set.add(entry.profileId())) {
                return true;
            }
        }

        return false;
    }

    private int calculateIndices(List<LastSeenMessages.Entry> lastSeen, int[] result, @Nullable LastSeenMessages.Entry lastReceived) {
        Arrays.fill(result, Integer.MIN_VALUE);
        List<LastSeenMessages.Entry> list = this.lastSeenMessages.entries();
        int i = list.size();

        for(int j = i - 1; j >= 0; --j) {
            int k = lastSeen.indexOf(list.get(j));
            if (k != -1) {
                result[k] = -j - 1;
            }
        }

        int l = Integer.MIN_VALUE;
        int m = this.pendingEntries.size();

        for(int n = 0; n < m; ++n) {
            LastSeenMessages.Entry entry = this.pendingEntries.get(n);
            int o = lastSeen.indexOf(entry);
            if (o != -1) {
                result[o] = n;
            }

            if (entry.equals(lastReceived)) {
                l = n;
            }
        }

        return l;
    }

    public Set<LastSeenMessagesValidator.ErrorCondition> validateAndUpdate(LastSeenMessages.Update acknowledgment) {
        EnumSet<LastSeenMessagesValidator.ErrorCondition> enumSet = EnumSet.noneOf(LastSeenMessagesValidator.ErrorCondition.class);
        LastSeenMessages lastSeenMessages = acknowledgment.lastSeen();
        LastSeenMessages.Entry entry = acknowledgment.lastReceived().orElse((LastSeenMessages.Entry)null);
        List<LastSeenMessages.Entry> list = lastSeenMessages.entries();
        int i = this.lastSeenMessages.entries().size();
        int j = Integer.MIN_VALUE;
        int k = list.size();
        if (k < i) {
            enumSet.add(LastSeenMessagesValidator.ErrorCondition.REMOVED_MESSAGES);
        }

        int[] is = new int[k];
        int l = this.calculateIndices(list, is, entry);

        for(int m = k - 1; m >= 0; --m) {
            int n = is[m];
            if (n != Integer.MIN_VALUE) {
                if (n < j) {
                    enumSet.add(LastSeenMessagesValidator.ErrorCondition.OUT_OF_ORDER);
                } else {
                    j = n;
                }
            } else {
                enumSet.add(LastSeenMessagesValidator.ErrorCondition.UNKNOWN_MESSAGES);
            }
        }

        if (entry != null) {
            if (l != Integer.MIN_VALUE && l >= j) {
                j = l;
            } else {
                enumSet.add(LastSeenMessagesValidator.ErrorCondition.UNKNOWN_MESSAGES);
            }
        }

        if (j >= 0) {
            this.pendingEntries.removeElements(0, j + 1);
        }

        if (this.hasDuplicateProfiles(lastSeenMessages)) {
            enumSet.add(LastSeenMessagesValidator.ErrorCondition.DUPLICATED_PROFILES);
        }

        this.lastSeenMessages = lastSeenMessages;
        return enumSet;
    }

    public static enum ErrorCondition {
        OUT_OF_ORDER("messages received out of order"),
        DUPLICATED_PROFILES("multiple entries for single profile"),
        UNKNOWN_MESSAGES("unknown message"),
        REMOVED_MESSAGES("previously present messages removed from context");

        private final String message;

        private ErrorCondition(String description) {
            this.message = description;
        }

        public String message() {
            return this.message;
        }
    }
}
