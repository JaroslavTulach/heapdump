package heap.language.heap;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.util.Descriptors;
import heap.language.util.HeapLanguageUtils;
import org.netbeans.api.annotations.common.NonNull;

import java.util.Iterator;

/**
 * A wrapper around {@link Iterator} interface which allows interoperability with JavaScript iterators
 * via truffle message interface.
 *
 * The iterator elements should be either valid interop values, or instances of
 * {@link org.netbeans.lib.profiler.heap.Instance} or {@link org.netbeans.lib.profiler.heap.JavaClass}.
 */
@ExportLibrary(InteropLibrary.class)
public class IteratorWrapperObject implements TruffleObject, Iterator<Object> {

    private static final String DONE = "done";
    private static final String NEXT = "next";

    private static final Descriptors MEMBERS = Descriptors.build(new String[]{ DONE }, new String[] { NEXT });

    @NonNull
    private final Iterator<Object> iterator;

    public <T> IteratorWrapperObject(@NonNull Iterator<T> iterator) {
        //noinspection unchecked - currently, interop library is not particularly friendly to generic heap
        this.iterator = (Iterator<Object>) iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Object next() {
        return iterator.next();
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") IteratorWrapperObject receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(@SuppressWarnings("unused") IteratorWrapperObject receiver, @SuppressWarnings("unused") boolean includeInternal) {
        return MEMBERS;
    }

    @ExportMessage
    static boolean isMemberInvocable(@SuppressWarnings("unused") IteratorWrapperObject receiver, String member) {
        return MEMBERS.hasFunction(member);
    }

    @ExportMessage
    static boolean isMemberReadable(@SuppressWarnings("unused") IteratorWrapperObject receiver, String member) {
        return MEMBERS.hasProperty(member);
    }

    @ExportMessage
    static Object invokeMember(IteratorWrapperObject receiver, String member, Object[] arguments)
            throws UnknownIdentifierException, ArityException {
        if (NEXT.equals(member)) {
            HeapLanguageUtils.arityCheck(0, arguments);
            // TODO: We should actually return an extra wrapper object (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Iteration_protocols)
            return HeapLanguageUtils.heapToTruffle(receiver.iterator.next());
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    static Object readMember(IteratorWrapperObject receiver, String member) throws UnknownIdentifierException {
        if (DONE.equals(member)) {
            return !receiver.iterator.hasNext();
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

}
