# Flex Algorithm Notes

## Definitions

**Main Axis**: The direction along the axis of the flex element. In our case, that's X for row(), Y for column().

**Cross Axis**: The other direction, i.e. the direction along the axis orthogonal to the main axis.

**Main Size / Cross Size**: The dimensions of the element along the given axis.

**Main Start, Main End, Cross Start, Cross End**: The positions at which the container starts and ends along the given
axis. These are one dimensional units, so the main start position is the leftmost X position for a row() flex element.

**Flex Container**: The parent flex object.

**Flex Item**: Child flex objects that are fit within the container (parent).

**Definite Size**: A size that can be determined before performing layout. This can include typically indefinite sizes (
such as percentages) that are immediately within a definitely sized parent flex object.

## Line Length

### Determine available main/cross space for flex items

This calculates the total available space for any children of this object. This is the area inside the container, as
well as within its margin, border, and padding. At this point, because this is a recursive process, the current object's
available space will already be known.

For each dimension (main, cross):

- If the size for the dimension is definite, use that value for this dimension
- Otherwise, subtract the margin, border, and padding from this container's available space, and use that value.
- Note that any of these values can be **infinite**.

Assign the result to all children as their available space.

### Main Size Determination



