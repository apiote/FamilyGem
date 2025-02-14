package app.familygem.detail;

import android.app.Activity;
import android.text.InputType;

import org.folg.gedcom.model.Note;

import app.familygem.DetailActivity;
import app.familygem.Global;
import app.familygem.Memory;
import app.familygem.R;
import app.familygem.U;
import app.familygem.util.ChangeUtils;
import app.familygem.visitor.NoteReferences;

public class NoteActivity extends DetailActivity {

    Note note;

    @Override
    public void format() {
        note = (Note)cast(Note.class);
        if (note.getId() == null) {
            setTitle(R.string.note);
            placeSlug("NOTE");
        } else {
            setTitle(R.string.shared_note);
            placeSlug("NOTE", note.getId());
        }
        place(getString(R.string.text), "Value", true, InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        place(getString(R.string.rin), "Rin", false, 0);
        placeExtensions(note);
        U.placeSourceCitations(box, note);
        U.placeChangeDate(box, note.getChange());
        if (note.getId() != null) {
            NoteReferences noteRefs = new NoteReferences(Global.gc, note.getId(), false);
            if (noteRefs.count > 0)
                U.placeCabinet(box, noteRefs.leaders.toArray(), R.string.shared_by);
        } else if (((Activity)box.getContext()).getIntent().getBooleanExtra("fromNotes", false)) {
            U.placeCabinet(box, Memory.getLeaderObject(), R.string.written_in);
        }
    }

    @Override
    public void delete() {
        ChangeUtils.INSTANCE.updateChangeDate(U.deleteNote(note, null));
    }
}
