package app.familygem.detail;

import android.text.InputType;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.PersonFamilyCommonContainer;

import java.util.Arrays;

import app.familygem.DetailActivity;
import app.familygem.Memory;
import app.familygem.ProfileFactsFragment;
import app.familygem.R;
import app.familygem.U;
import app.familygem.util.ChangeUtils;

public class EventActivity extends DetailActivity {

    EventFact event;
    /**
     * List of event tags useful to avoid putting the Value of the EventFact.
     */
    String[] eventTags = {"BIRT", "CHR", "DEAT", "BURI", "CREM", "ADOP", "BAPM", "BARM", "BASM", "BLES", // Individual events
            "CHRA", "CONF", "FCOM", "ORDN", "NATU", "EMIG", "IMMI", "CENS", "PROB", "WILL", "GRAD", "RETI",
            "ANUL", "DIV", "DIVF", "ENGA", "MARB", "MARC", "MARR", "MARL", "MARS"}; // Family events

    @Override
    public void format() {
        event = (EventFact)cast(EventFact.class);
        if (Memory.getLeaderObject() instanceof Family)
            setTitle(writeEventTitle((Family)Memory.getLeaderObject(), event));
        else
            setTitle(ProfileFactsFragment.writeEventTitle(event)); // The title includes event.getDisplayType()
        placeSlug(event.getTag());
        if (Arrays.asList(eventTags).contains(event.getTag())) // It's an event (without Value)
            place(getString(R.string.value), "Value", false, 0);
        else // All other cases, usually attributes (with Value)
            place(getString(R.string.value), "Value");
        if (event.getTag().equals("MARR"))
            place(getString(R.string.type), "Type"); // Type of relationship
        else
            place(getString(R.string.type), "Type", event.getTag().equals("EVEN"), 0);
        place(getString(R.string.date), "Date");
        place(getString(R.string.place), "Place");
        place(getString(R.string.address), event.getAddress());
        place(getString(R.string.cause), "Cause", event.getTag() != null && event.getTag().equals("DEAT"), 0);
        place(getString(R.string.www), "Www", false, InputType.TYPE_CLASS_TEXT);
        place(getString(R.string.email), "Email", false, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        place(getString(R.string.telephone), "Phone", false, InputType.TYPE_CLASS_PHONE);
        place(getString(R.string.fax), "Fax", false, InputType.TYPE_CLASS_PHONE);
        place(getString(R.string.rin), "Rin", false, 0);
        place(getString(R.string.user_id), "Uid", false, 0);
        // Other methods are "WwwTag", "EmailTag", "UidTag"
        placeExtensions(event);
        U.placeNotes(box, event, true);
        U.placeMedia(box, event, true);
        U.placeSourceCitations(box, event);
    }

    @Override
    public void delete() {
        ((PersonFamilyCommonContainer)Memory.getSecondToLastObject()).getEventsFacts().remove(event);
        ChangeUtils.INSTANCE.updateChangeDate(Memory.getLeaderObject());
        Memory.setInstanceAndAllSubsequentToNull(event);
    }

    /**
     * Deletes the main empty tags and possibly sets 'Y' as value.
     */
    public static void cleanUpTag(EventFact ef) {
        if (ef.getType() != null && ef.getType().isEmpty()) ef.setType(null);
        if (ef.getDate() != null && ef.getDate().isEmpty()) ef.setDate(null);
        if (ef.getPlace() != null && ef.getPlace().isEmpty()) ef.setPlace(null);
        String tag = ef.getTag();
        if (tag != null && (tag.equals("BIRT") || tag.equals("CHR") || tag.equals("DEAT")
                || tag.equals("MARR") || tag.equals("DIV"))) {
            if (ef.getType() == null && ef.getDate() == null && ef.getPlace() == null
                    && ef.getAddress() == null && ef.getCause() == null)
                ef.setValue("Y");
            else
                ef.setValue(null);
        }
        if (ef.getValue() != null && ef.getValue().isEmpty()) ef.setValue(null);
    }
}
