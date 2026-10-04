package org.lovetropics.dimensions.duck;

import org.jetbrains.annotations.ApiStatus;
import org.lovetropics.dimensions.SharedDimensionState;

@ApiStatus.Internal
public interface SharedDimensionStateBindable {
	void ltdimensions$bindTo(SharedDimensionState sharedState);
}
