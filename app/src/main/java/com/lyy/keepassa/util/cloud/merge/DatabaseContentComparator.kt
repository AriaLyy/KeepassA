package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwDatabase

class DatabaseContentComparator(
  private val collector: DbDiffCollector = DbDiffCollector(),
  private val orderSynchronizer: GroupChildOrderSynchronizer = GroupChildOrderSynchronizer()
) {
  fun sameContent(cloudDb: PwDatabase, localDb: PwDatabase): Boolean {
    val differences = collector.collect(cloudDb, localDb)
    return differences.newList.isEmpty() &&
        differences.moveList.isEmpty() &&
        differences.delList.isEmpty() &&
        differences.modifyList.isEmpty() &&
        !orderSynchronizer.hasDifferences(cloudDb, localDb)
  }
}
